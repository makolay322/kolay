package app.kolay.payload.tools.halal;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import app.kolay.payload.tools.qr.QrCamera;
import app.kolay.payload.ui.*;

/**
 * Shared camera page of the halal tool: header, camera card with torch, permission / error state and a footer slot.
 * The camera runs only while the screen is on top and in the foreground; frames are never stored.
 */
abstract class CamScreen extends Screen implements QrCamera.Listener {
    private static final int REQ_CAMERA = 72;

    protected QrCamera cam;
    protected LinearLayout footer;
    private FrameLayout stage, cameraCard, torchBtn;
    private LinearLayout statePanel;
    private TextView stateTitle, stateText, stateBtn;
    private ImageView torchIcon;
    private boolean shown, asked, finished;

    abstract String title();
    abstract String subtitle();
    abstract QrCamera.Mode mode();
    /** True on the label (OCR) page, false on the barcode page; drives the mode switch under the header. */
    abstract boolean ocrMode();
    /** Fill the footer below the camera (hint text, buttons). */
    abstract void buildFooter(LinearLayout footer);
    /** Back button / header arrow. The camera is already stopped. */
    abstract void leave();

    @Override public View create() {
        Palette p = c.p;
        LinearLayout page = new LinearLayout(c.a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.addView(new Header(c, title(), subtitle(), this::back).view);

        page.addView(modeBar(), Ui.lp(-1, c.dp(44), c.dp(16), 0, c.dp(16), c.dp(8)));

        stage = new FrameLayout(c.a);
        stage.setPadding(c.dp(16), c.dp(4), c.dp(16), c.dp(12));

        cameraCard = new FrameLayout(c.a);
        Ui.surface(c, cameraCard, p.surface2, 22, false);
        cameraCard.setClipToOutline(true);
        TextureView tv = new TextureView(c.a);
        cameraCard.addView(tv, new FrameLayout.LayoutParams(-1, -1));
        if (mode() == QrCamera.Mode.PRODUCT) cameraCard.addView(new Guide(), new FrameLayout.LayoutParams(-1, -1));
        cam = new QrCamera(c.a, tv, this, mode());
        if (cam.hasTorch()) {
            torchBtn = new FrameLayout(c.a);
            torchIcon = new ImageView(c.a);
            torchBtn.addView(torchIcon, new FrameLayout.LayoutParams(c.dp(24), c.dp(24), Gravity.CENTER));
            torchBtn.setContentDescription("Fener"); torchBtn.setClickable(true);
            paintTorch(false);
            torchBtn.setOnClickListener(v -> {
                Ui.haptic(v, HapticFeedbackConstants.VIRTUAL_KEY);
                cam.setTorch(!cam.torchOn());
                paintTorch(cam.torchOn());
            });
            Ui.press(torchBtn);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(c.dp(52), c.dp(52), Gravity.BOTTOM | Gravity.END);
            lp.setMargins(0, 0, c.dp(16), c.dp(16));
            cameraCard.addView(torchBtn, lp);
        }
        stage.addView(cameraCard, new FrameLayout.LayoutParams(-1, -1));

        statePanel = new LinearLayout(c.a);
        statePanel.setOrientation(LinearLayout.VERTICAL);
        statePanel.setGravity(Gravity.CENTER);
        statePanel.setPadding(c.dp(24), 0, c.dp(24), 0);
        FrameLayout badge = new FrameLayout(c.a);
        badge.setBackground(Ui.rounded(c, p.primarySoft, 22));
        badge.addView(Ui.icon(c, Icon.Kind.CAMERA, p.onPrimarySoft, 32), new FrameLayout.LayoutParams(c.dp(32), c.dp(32), Gravity.CENTER));
        statePanel.addView(badge, new LinearLayout.LayoutParams(c.dp(72), c.dp(72)));
        stateTitle = Ui.text(c, "", 18, p.ink, 600);
        stateTitle.setGravity(Gravity.CENTER);
        statePanel.addView(stateTitle, Ui.lp(-2, -2, 0, c.dp(20), 0, 0));
        stateText = Ui.text(c, "", 14, p.muted, 400);
        stateText.setGravity(Gravity.CENTER); stateText.setLineSpacing(0, 1.25f);
        statePanel.addView(stateText, Ui.lp(-2, -2, 0, c.dp(8), 0, 0));
        stateBtn = Ui.button(c, "", Ui.Style.PRIMARY, null);
        statePanel.addView(stateBtn, Ui.lp(-2, c.dp(52), 0, c.dp(24), 0, 0));
        statePanel.setVisibility(View.GONE);
        stage.addView(statePanel, new FrameLayout.LayoutParams(-1, -1));
        page.addView(stage, new LinearLayout.LayoutParams(-1, 0, 1));

        footer = new LinearLayout(c.a);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        footer.setPadding(c.dp(16), 0, c.dp(16), c.dp(14));
        buildFooter(footer);
        page.addView(footer);
        return page;
    }

    /** Barkod | Etiket (OCR): barcode is the default; the other mode is only entered by choosing it. */
    private View modeBar() {
        Palette p = c.p;
        LinearLayout bar = new LinearLayout(c.a);
        bar.setBackground(Ui.rounded(c, p.surface2, 16));
        bar.setPadding(c.dp(4), c.dp(4), c.dp(4), c.dp(4));
        bar.addView(modeChip("Barkod", Icon.Kind.BARCODE, !ocrMode(), () -> { finish(); c.nav.replace(new ScanScreen(null)); }), new LinearLayout.LayoutParams(0, -1, 1));
        bar.addView(modeChip("Etiket (OCR)", Icon.Kind.CAMERA, ocrMode(), () -> { finish(); c.nav.replace(new LabelScreen(null, "İçindekiler yazısını çerçeveye al")); }), new LinearLayout.LayoutParams(0, -1, 1));
        return bar;
    }

    private TextView modeChip(String label, Icon.Kind icon, boolean selected, Runnable onTap) {
        Palette p = c.p;
        TextView t = Ui.text(c, label, 14, selected ? p.onPrimary : p.muted, selected ? 700 : 500);
        t.setGravity(Gravity.CENTER);
        t.setCompoundDrawablePadding(c.dp(6));
        android.graphics.drawable.Drawable d = new Icon(icon, selected ? p.onPrimary : p.muted);
        d.setBounds(0, 0, c.dp(18), c.dp(18));
        t.setCompoundDrawablesRelative(d, null, null, null);
        t.setClickable(true);
        if (selected) t.setBackground(Ui.rounded(c, p.primary, 12));
        else { t.setOnClickListener(v -> { Ui.haptic(v, HapticFeedbackConstants.CLOCK_TICK); onTap.run(); }); Ui.press(t); }
        return t;
    }

    private void back() { cam.stop(); leave(); }

    /** Call once the screen has what it came for: keeps the camera from restarting on resume. */
    protected final void finish() { finished = true; cam.stop(); paintTorch(false); }

    // ---- lifecycle ------------------------------------------------------------------------------

    @Override public void onShown() { shown = true; ensureCamera(); }
    @Override public void onResume() { if (shown) ensureCamera(); }
    @Override public void onPause() { cam.stop(); paintTorch(false); }
    @Override public void onDestroy() { cam.stop(); }
    @Override public boolean onBack() { back(); return true; }

    private boolean granted() { return c.a.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED; }

    private void ensureCamera() {
        if (finished || !shown) return;
        if (granted()) {
            statePanel.setVisibility(View.GONE);
            cameraCard.setVisibility(View.VISIBLE);
            footer.setVisibility(View.VISIBLE);
            cam.start();
        } else if (!asked) {
            asked = true;
            c.a.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        } else {
            showState("Kamera izni gerekli", "Kamerayı kullanmak için erişim izni ver.",
                c.a.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ? "İzin ver" : "Ayarları aç", this::askOrSettings);
        }
    }

    private void askOrSettings() {
        if (c.a.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            c.a.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        } else {
            c.a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", c.a.getPackageName(), null)));
        }
    }

    @Override public void onPermission(int req, String[] perms, int[] results) {
        if (req == REQ_CAMERA) ensureCamera();
    }

    @Override public void onError(String message) {
        showState("Kamera kullanılamıyor", message, "Tekrar dene", this::ensureCamera);
    }

    @Override public void onCode(String text) {}

    private void showState(String title, String text, String btn, Runnable action) {
        cam.stop();
        cameraCard.setVisibility(View.GONE);
        footer.setVisibility(View.GONE);
        stateTitle.setText(title); stateText.setText(text); stateBtn.setText(btn);
        stateBtn.setOnClickListener(v -> action.run());
        if (statePanel.getVisibility() != View.VISIBLE) { statePanel.setVisibility(View.VISIBLE); Ui.appear(statePanel, 0); }
    }

    // ---- torch + guide --------------------------------------------------------------------------

    private void paintTorch(boolean on) {
        if (torchBtn == null) return;
        torchBtn.setBackground(Ui.rounded(c, on ? c.p.primary : 0x99000000, 26));
        torchIcon.setImageDrawable(new Icon(Icon.Kind.FLASH, on ? c.p.onPrimary : Color.WHITE));
    }

    /** Wide corner brackets: a purely visual aim guide for a barcode (the whole frame is decoded). */
    private final class Guide extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Guide() {
            super(c.a);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(c.dp(4)); paint.setColor(Color.WHITE);
        }
        @Override protected void onDraw(Canvas cv) {
            float w = getWidth() * 0.80f, h = w * 0.5f, arm = h * 0.22f;
            float l = (getWidth() - w) / 2f, t = (getHeight() - h) / 2f, r = l + w, b = t + h;
            cv.drawLines(new float[]{
                l, t + arm, l, t,  l, t, l + arm, t,
                r - arm, t, r, t,  r, t, r, t + arm,
                r, b - arm, r, b,  r, b, r - arm, b,
                l + arm, b, l, b,  l, b, l, b - arm}, paint);
        }
    }
}
