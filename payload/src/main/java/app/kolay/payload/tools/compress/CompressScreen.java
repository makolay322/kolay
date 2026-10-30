package app.kolay.payload.tools.compress;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Outline;
import android.media.ExifInterface;
import android.os.ParcelFileDescriptor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Size;
import android.view.*;
import android.widget.*;
import app.kolay.payload.ui.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/** Recent photos as a grid; tap one to save a 90% JPEG copy into Pictures/Kolay. */
final class CompressScreen extends Screen {
    private static final int MAX_PHOTOS = 90;
    private static final int QUALITY = 90;

    private final List<Uri> photos = new ArrayList<>();
    private FrameLayout content;
    private boolean destroyed;

    @Override public View create() {
        LinearLayout page = new LinearLayout(c.a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.addView(new Header(c, "Fotoğraf Sıkıştır", "Bir fotoğraf seç — %" + QUALITY + " JPEG kopyası kaydedilir", () -> c.nav.pop()).view);
        content = new FrameLayout(c.a);
        page.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        if (c.a.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) showGrid();
        else c.a.requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 1);
        return page;
    }

    @Override public void onPermission(int req, String[] perms, int[] r) {
        if (r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) showGrid();
        else message("Fotoğraflara erişim izni gerekli", "Ayarlar → Uygulamalar → Kolay → İzinler");
    }

    @Override public void onDestroy() { destroyed = true; }

    private void message(String title, String body) {
        LinearLayout l = new LinearLayout(c.a);
        l.setOrientation(LinearLayout.VERTICAL); l.setGravity(Gravity.CENTER);
        l.setPadding(c.dp(32), 0, c.dp(32), c.dp(64));
        TextView t = Ui.text(c, title, 18, c.p.ink, 600); t.setGravity(Gravity.CENTER);
        TextView b = Ui.text(c, body, 14, c.p.muted, 400); b.setGravity(Gravity.CENTER);
        l.addView(t); l.addView(b, Ui.lp(-2, -2, 0, c.dp(8), 0, 0));
        content.removeAllViews();
        content.addView(l, new FrameLayout.LayoutParams(-1, -1));
        Ui.appear(l, 0);
    }

    private void showGrid() {
        photos.clear();
        try (Cursor cur = c.a.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID}, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC")) {
            while (cur != null && cur.moveToNext() && photos.size() < MAX_PHOTOS)
                photos.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cur.getLong(0)));
        } catch (Exception ignored) { }
        if (photos.isEmpty()) { message("Fotoğraf bulunamadı", "Çekilen fotoğraflar burada görünür"); return; }

        GridView grid = new GridView(c.a);
        grid.setNumColumns(3);
        grid.setHorizontalSpacing(c.dp(6)); grid.setVerticalSpacing(c.dp(6));
        grid.setPadding(c.dp(12), c.dp(4), c.dp(12), c.dp(24));
        grid.setClipToPadding(false); grid.setSelector(android.R.color.transparent);
        grid.setVerticalScrollBarEnabled(false);
        grid.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return photos.size(); }
            @Override public Object getItem(int i) { return photos.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View v, ViewGroup parent) {
                Cell cell = v instanceof Cell ? (Cell) v : new Cell();
                cell.bind(photos.get(i));
                return cell;
            }
        });
        grid.setOnItemClickListener((p, v, i, id) -> {
            Ui.haptic(v, HapticFeedbackConstants.CONFIRM);
            compress(photos.get(i));
        });
        content.removeAllViews();
        content.addView(grid, new FrameLayout.LayoutParams(-1, -1));
    }

    private void compress(Uri src) {
        message("Sıkıştırılıyor…", "Fotoğraf yeniden kodlanıyor");
        c.io.execute(() -> {
            String error = null; Uri out = null; long before = size(src), after = 0;
            try {
                // ImageDecoder applies EXIF rotation, so the copy's orientation tag is reset to normal below.
                Bitmap bm = ImageDecoder.decodeBitmap(ImageDecoder.createSource(c.a.getContentResolver(), src),
                        (d, info, s) -> d.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE));
                Bitmap flat = Bitmap.createBitmap(bm.getWidth(), bm.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas cv = new Canvas(flat);
                cv.drawColor(0xFFFFFFFF);   // JPEG has no alpha
                cv.drawBitmap(bm, 0, 0, null);
                bm.recycle();
                ContentValues cvs = new ContentValues();
                cvs.put(MediaStore.Images.Media.DISPLAY_NAME, "Kolay_" + System.currentTimeMillis() + ".jpg");
                cvs.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                cvs.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Kolay");
                cvs.put(MediaStore.Images.Media.IS_PENDING, 1);
                out = c.a.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cvs);
                if (out == null) throw new Exception("Dosya oluşturulamadı");
                try (OutputStream os = c.a.getContentResolver().openOutputStream(out)) {
                    if (!flat.compress(Bitmap.CompressFormat.JPEG, QUALITY, os)) throw new Exception("Sıkıştırma başarısız");
                }
                flat.recycle();
                copyExif(src, out);
                ContentValues done = new ContentValues();
                done.put(MediaStore.Images.Media.IS_PENDING, 0);
                c.a.getContentResolver().update(out, done, null, null);
                after = size(out);
            } catch (Throwable t) {
                if (out != null) try { c.a.getContentResolver().delete(out, null, null); } catch (Exception ignored) { }
                out = null;
                error = t.getMessage() == null ? "Sıkıştırma başarısız" : t.getMessage();
            }
            final Uri result = out; final String err = error; final long b = before, a = after;
            c.ui.post(() -> { if (!destroyed) showResult(result, err, b, a); });
        });
    }

    private void showResult(Uri out, String error, long before, long after) {
        if (out == null) { message("Olmadı", error); return; }
        LinearLayout l = new LinearLayout(c.a);
        l.setOrientation(LinearLayout.VERTICAL); l.setGravity(Gravity.CENTER);
        l.setPadding(c.dp(32), 0, c.dp(32), c.dp(32));
        TextView t = Ui.text(c, "Kaydedildi", 20, c.p.ink, 700); t.setGravity(Gravity.CENTER);
        String sizes = before > 0 ? kb(before) + " → " + kb(after) : kb(after);
        TextView b = Ui.text(c, sizes + "\nPictures/Kolay klasörüne yeni kopya olarak eklendi; özgün fotoğraf korunur.", 14, c.p.muted, 400);
        b.setGravity(Gravity.CENTER);
        l.addView(t); l.addView(b, Ui.lp(-2, -2, 0, c.dp(8), 0, c.dp(24)));
        TextView share = Ui.button(c, "Paylaş", Ui.Style.TONAL, Icon.Kind.SHARE);
        share.setOnClickListener(v -> c.a.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
                .setType("image/jpeg").putExtra(Intent.EXTRA_STREAM, out).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null)));
        TextView again = Ui.button(c, "Başka fotoğraf", Ui.Style.PRIMARY, Icon.Kind.SCAN);
        again.setOnClickListener(v -> showGrid());
        l.addView(share, Ui.lp(-1, c.dp(52)));
        l.addView(again, Ui.lp(-1, c.dp(52), 0, c.dp(12), 0, 0));
        content.removeAllViews();
        content.addView(l, new FrameLayout.LayoutParams(-1, -1));
        Ui.appear(l, 0);
    }

    /** Camera/date/GPS tags worth carrying over; size and thumbnail tags are deliberately left out. */
    private static final String[] EXIF_TAGS = {
        ExifInterface.TAG_DATETIME, ExifInterface.TAG_DATETIME_ORIGINAL, ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_SUBSEC_TIME, ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
        ExifInterface.TAG_OFFSET_TIME, ExifInterface.TAG_OFFSET_TIME_ORIGINAL, ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
        ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL, ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_LENS_MAKE, ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_IMAGE_DESCRIPTION, ExifInterface.TAG_ARTIST, ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_EXPOSURE_TIME, ExifInterface.TAG_F_NUMBER, ExifInterface.TAG_EXPOSURE_PROGRAM,
        ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, ExifInterface.TAG_EXPOSURE_BIAS_VALUE, ExifInterface.TAG_MAX_APERTURE_VALUE,
        ExifInterface.TAG_METERING_MODE, ExifInterface.TAG_LIGHT_SOURCE, ExifInterface.TAG_FLASH,
        ExifInterface.TAG_FOCAL_LENGTH, ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF, ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF, ExifInterface.TAG_GPS_TIMESTAMP, ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
    };

    /** Best effort: a failure here must not lose the compressed copy. */
    private void copyExif(Uri src, Uri dst) {
        try (InputStream in = c.a.getContentResolver().openInputStream(src);
             ParcelFileDescriptor pfd = c.a.getContentResolver().openFileDescriptor(dst, "rw")) {
            ExifInterface from = new ExifInterface(in);
            ExifInterface to = new ExifInterface(pfd.getFileDescriptor());
            for (String tag : EXIF_TAGS) {
                String v = from.getAttribute(tag);
                if (v != null) to.setAttribute(tag, v);
            }
            to.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
            to.saveAttributes();
        } catch (Exception ignored) { }
    }

    private long size(Uri u) {
        try (Cursor cur = c.a.getContentResolver().query(u, new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (cur != null && cur.moveToFirst()) return cur.getLong(0);
        } catch (Exception ignored) { }
        return 0;
    }

    private static String kb(long bytes) {
        return bytes >= 1_000_000 ? String.format(java.util.Locale.US, "%.1f MB", bytes / 1e6) : (bytes / 1000) + " KB";
    }

    /** Square thumbnail. */
    private final class Cell extends FrameLayout {
        final ImageView img; Uri uri;

        Cell() {
            super(c.a);
            final float r = c.dp(12);
            img = new ImageView(c.a);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setBackgroundColor(c.p.surface2);
            img.setClipToOutline(true);
            img.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r); }
            });
            addView(img, new LayoutParams(-1, -1));
            Ui.press(this);
        }

        @Override protected void onMeasure(int w, int h) { super.onMeasure(w, w); setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth()); }

        void bind(Uri u) {
            uri = u;
            img.setImageDrawable(null);
            c.io.execute(() -> {
                try {
                    Bitmap t = c.a.getContentResolver().loadThumbnail(u, new Size(320, 320), null);
                    c.ui.post(() -> {
                        if (u.equals(uri)) { img.setImageBitmap(t); img.setAlpha(0f); img.animate().alpha(1f).setDuration(220).start(); }
                    });
                } catch (Exception ignored) { }
            });
        }
    }
}
