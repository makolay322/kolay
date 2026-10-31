package app.kolay.payload.tools.qr;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * Back camera → preview on a TextureView + a YUV stream that is decoded for QR codes ({@link Mode#QR}), retail barcodes
 * ({@link Mode#PRODUCT}), or kept undecoded until {@link #capture()} grabs one sharp grayscale still ({@link Mode#TEXT}, for OCR).
 * Each start() gets its own {@link Run} (thread, device, session), so a quick stop/start can never close the newer session.
 * Only the newest frame is decoded, older ones are dropped, so scanning never lags behind. Frames are never stored or logged.
 */
public final class QrCamera {
    public enum Mode { QR, PRODUCT, TEXT }

    public interface Listener {
        /** Main thread. Delivered once per start(). */
        void onCode(String text);
        /** Main thread. {@link Mode#TEXT} only: the still requested by {@link #capture()}, upright, grayscale. The receiver owns the bitmap. */
        default void onStill(Bitmap still) {}
        /** Main thread. The camera has been released. */
        void onError(String message);
    }

    private final TextureView tv;
    private final Listener l;
    private final Mode mode;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final CameraManager cm;
    private final String id;            // back camera, or null when the device has none
    private final CameraCharacteristics ch;
    private final Size size;
    private final boolean torchAble;

    private Run cur;                    // main thread only
    private boolean torch;              // main thread only

    public QrCamera(Activity a, TextureView tv, Listener l) { this(a, tv, l, Mode.QR); }

    public QrCamera(Activity a, TextureView tv, Listener l, Mode mode) {
        this.tv = tv; this.l = l; this.mode = mode;
        CameraManager m = null; String back = null; CameraCharacteristics c = null; Size s = null; boolean flash = false;
        try {
            m = (CameraManager) a.getSystemService(Context.CAMERA_SERVICE);
            for (String cid : m.getCameraIdList()) {
                CameraCharacteristics cc = m.getCameraCharacteristics(cid);
                Integer facing = cc.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) { back = cid; c = cc; break; }
            }
            if (c != null) {
                s = pickSize(c, mode);
                Boolean f = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                flash = f != null && f;
            }
        } catch (Exception ignored) { back = null; }
        cm = m; id = back; ch = c; size = s; torchAble = flash;

        tv.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int hgt) { fit(w, hgt); if (cur != null) cur.open(); }
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int hgt) { fit(w, hgt); }
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture st) { stop(); return true; }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture st) { }
        });
    }

    public boolean available() { return id != null && size != null; }
    public boolean hasTorch() { return torchAble; }
    public boolean torchOn() { return torch; }

    /**
     * QR / product: largest 16:9 stream up to 1280 px wide (plenty for a code, cheap to decode); otherwise the largest ≤ ~1.2 MP that
     * preview and analysis both support. TEXT: the largest stream up to ~3.5 MP (small print needs the pixels).
     */
    private static Size pickSize(CameraCharacteristics c, Mode mode) {
        StreamConfigurationMap map = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) return null;
        Size[] yuv = map.getOutputSizes(ImageFormat.YUV_420_888), prev = map.getOutputSizes(SurfaceTexture.class);
        if (yuv == null || prev == null) return null;
        List<Size> pl = Arrays.asList(prev);
        Size wide = null, any = null;
        long cap = mode == Mode.TEXT ? 3_500_000L : 1_230_000L;
        for (Size s : yuv) {
            if (!pl.contains(s)) continue;
            long area = (long) s.getWidth() * s.getHeight();
            if (area <= cap && (any == null || area > (long) any.getWidth() * any.getHeight())) any = s;
            boolean sixteenNine = Math.abs(s.getWidth() / (float) s.getHeight() - 16f / 9f) < 0.05f;
            if (mode != Mode.TEXT && sixteenNine && s.getWidth() <= 1280 && (wide == null || s.getWidth() > wide.getWidth())) wide = s;
        }
        return wide != null ? wide : any;
    }

    /** The activity is portrait-locked, so the SurfaceTexture arrives upright but stretched to the view; undo the stretch and crop to fill. */
    private void fit(int vw, int vh) {
        if (size == null || vw == 0 || vh == 0) return;
        Integer so = ch.get(CameraCharacteristics.SENSOR_ORIENTATION);
        boolean swap = so != null && so % 180 == 90;
        float cw = swap ? size.getHeight() : size.getWidth(), chh = swap ? size.getWidth() : size.getHeight();
        float s = Math.max(vw / cw, vh / chh);
        Matrix m = new Matrix();
        m.setScale(cw * s / vw, chh * s / vh, vw / 2f, vh / 2f);
        tv.setTransform(m);
    }

    // ---- lifecycle (main thread) -----------------------------------------------------------------

    /** Needs the CAMERA permission. Idempotent. */
    public void start() {
        if (cur != null) return;
        if (!available()) { l.onError("Bu cihazda arka kamera bulunamadı."); return; }
        cur = new Run();
        if (tv.isAvailable()) { fit(tv.getWidth(), tv.getHeight()); cur.open(); }
        // otherwise the surface listener opens the camera once the surface exists
    }

    /** Releases the camera (the privacy indicator goes off). Idempotent. */
    public void stop() {
        Run r = cur;
        cur = null; torch = false;
        if (r != null) r.close();
    }

    public void setTorch(boolean on) {
        if (!torchAble || cur == null) return;
        torch = on;
        cur.applyTorch(on);
    }

    /** {@link Mode#TEXT}: delivers the next frame through {@link Listener#onStill}. Give autofocus a moment before calling. */
    public void capture() {
        Run r = cur;
        if (r != null && mode == Mode.TEXT) r.wantStill = true;
    }

    private void fail(Run r, String msg) {
        main.post(() -> {
            if (cur != r) return;
            stop();
            l.onError(msg);
        });
    }

    /** One camera session with its own thread and resources. */
    private final class Run {
        private final HandlerThread th = new HandlerThread("qr-cam");
        private final Handler h;
        private final QrDecoder decoder = mode == Mode.TEXT ? null : new QrDecoder(mode == Mode.PRODUCT);
        private volatile boolean alive = true, opening, inFlight, done, wantStill;
        // camera thread only:
        private CameraDevice dev;
        private CameraCaptureSession sess;
        private ImageReader reader;
        private Surface previewSurface;
        private CaptureRequest.Builder req;
        private boolean wantTorch;

        Run() { th.start(); h = new Handler(th.getLooper()); }

        void open() {
            if (opening || !alive) return;
            opening = true;
            h.post(this::openOnThread);
        }

        @SuppressWarnings("MissingPermission")  // the screen requests CAMERA before calling start()
        private void openOnThread() {
            inFlight = true;  // until a StateCallback fires; the thread must outlive it so a late onOpened can still close the device
            try {
                cm.openCamera(id, new CameraDevice.StateCallback() {
                    @Override public void onOpened(CameraDevice cd) {
                        inFlight = false;
                        if (!alive) { cd.close(); return; }
                        dev = cd;
                        createSession();
                    }
                    @Override public void onDisconnected(CameraDevice cd) { inFlight = false; cd.close(); fail(Run.this, "Kamera başka bir uygulama tarafından alındı."); }
                    @Override public void onError(CameraDevice cd, int err) { inFlight = false; cd.close(); fail(Run.this, "Kamera açılamadı."); }
                    @Override public void onClosed(CameraDevice cd) { th.quitSafely(); }
                }, h);
            } catch (Exception e) { inFlight = false; fail(this, "Kamera açılamadı."); }
        }

        private void createSession() {
            try {
                SurfaceTexture st = tv.getSurfaceTexture();
                if (st == null) { fail(this, "Kamera önizlemesi hazırlanamadı."); return; }
                st.setDefaultBufferSize(size.getWidth(), size.getHeight());
                previewSurface = new Surface(st);
                reader = ImageReader.newInstance(size.getWidth(), size.getHeight(), ImageFormat.YUV_420_888, 2);
                reader.setOnImageAvailableListener(this::onFrame, h);

                req = dev.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                req.addTarget(previewSurface);
                req.addTarget(reader.getSurface());
                int[] af = ch.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
                if (af != null && Arrays.stream(af).anyMatch(m -> m == CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE))
                    req.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                SessionConfiguration sc = new SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
                    Arrays.asList(new OutputConfiguration(previewSurface), new OutputConfiguration(reader.getSurface())),
                    h::post,
                    new CameraCaptureSession.StateCallback() {
                        @Override public void onConfigured(CameraCaptureSession s) {
                            if (!alive) { s.close(); return; }
                            sess = s;
                            repeat();
                        }
                        @Override public void onConfigureFailed(CameraCaptureSession s) { fail(Run.this, "Kamera başlatılamadı."); }
                    });
                dev.createCaptureSession(sc);
            } catch (Exception e) { fail(this, "Kamera başlatılamadı."); }
        }

        private void repeat() {
            if (sess == null || req == null) return;
            try {
                req.set(CaptureRequest.FLASH_MODE, wantTorch ? CameraMetadata.FLASH_MODE_TORCH : CameraMetadata.FLASH_MODE_OFF);
                sess.setRepeatingRequest(req.build(), null, h);
            } catch (Exception ignored) { /* session closed meanwhile */ }
        }

        void applyTorch(boolean on) { h.post(() -> { wantTorch = on; repeat(); }); }

        private void onFrame(ImageReader r) {
            Image img = null;
            try {
                img = r.acquireLatestImage();
                if (img == null || done || !alive) return;
                if (mode == Mode.TEXT && !wantStill) return;
                Image.Plane y = img.getPlanes()[0];
                int w = img.getWidth(), hgt = img.getHeight(), stride = y.getRowStride();
                ByteBuffer buf = y.getBuffer();
                byte[] data = new byte[stride * hgt];
                buf.get(data, 0, Math.min(buf.remaining(), data.length));
                if (mode == Mode.TEXT) {
                    wantStill = false; done = true;
                    Bitmap still = upright(data, stride, w, hgt);
                    main.post(() -> { if (cur == this) l.onStill(still); else still.recycle(); });
                    return;
                }
                String text = decoder.decode(data, stride, w, hgt);
                if (text != null && !text.isEmpty()) {
                    done = true;
                    main.post(() -> { if (cur == this) l.onCode(text); });
                }
            } catch (Exception ignored) {
                // a bad frame must not end scanning
            } finally {
                if (img != null) img.close();
            }
        }

        /** Luma plane -> upright grayscale bitmap (the sensor is mounted rotated; the activity is portrait-locked). */
        private Bitmap upright(byte[] data, int stride, int w, int h) {
            int[] px = new int[w * h];
            for (int r = 0; r < h; r++) {
                int base = r * stride, o = r * w;
                for (int x = 0; x < w; x++) { int v = data[base + x] & 0xFF; px[o + x] = 0xFF000000 | v << 16 | v << 8 | v; }
            }
            Bitmap raw = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888);
            Integer so = ch.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (so == null || so == 0) return raw;
            Matrix m = new Matrix();
            m.postRotate(so);
            Bitmap rot = Bitmap.createBitmap(raw, 0, 0, w, h, m, true);
            raw.recycle();
            return rot;
        }

        void close() {
            alive = false;
            h.post(() -> {
                try { if (sess != null) sess.close(); } catch (Exception ignored) { }
                try { if (dev != null) dev.close(); } catch (Exception ignored) { }
                try { if (reader != null) reader.close(); } catch (Exception ignored) { }
                if (previewSurface != null) previewSurface.release();
                boolean hadDevice = dev != null;
                sess = null; dev = null; reader = null; previewSurface = null; req = null;
                if (!hadDevice && !inFlight) th.quitSafely();  // otherwise onClosed ends the thread
            });
        }
    }
}
