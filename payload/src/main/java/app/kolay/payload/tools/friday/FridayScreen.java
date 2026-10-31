package app.kolay.payload.tools.friday;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import app.kolay.payload.ui.*;
import java.io.OutputStream;
import java.util.Random;

/** Preview of the generated card with new-image / new-hadith / orientation / save / share. Nothing is stored until Save or Share. */
final class FridayScreen extends Screen {
    private final Random rnd = new Random();
    private long seed = rnd.nextLong();
    private int hadith = rnd.nextInt(Hadiths.ALL.length);
    private boolean portrait;
    private Bitmap bmp;
    private Uri saved;          // set once the current image was written to Pictures/Kolay
    private int gen;            // guards against stale renders finishing out of order
    private boolean destroyed;
    private ImageView preview;
    private TextView orient;

    @Override public View create() {
        LinearLayout page = new LinearLayout(c.a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.addView(new Header(c, "Cuma Mesajı", "Rastgele görsel · hadis · 3:2", () -> c.nav.pop()).view);

        FrameLayout box = new FrameLayout(c.a);
        box.setPadding(c.dp(16), c.dp(4), c.dp(16), c.dp(8));
        preview = new ImageView(c.a);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setAdjustViewBounds(true);
        box.addView(preview, new FrameLayout.LayoutParams(-1, -1));
        page.addView(box, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout f = new LinearLayout(c.a);
        f.setOrientation(LinearLayout.VERTICAL);
        f.setPadding(c.dp(20), c.dp(4), c.dp(20), c.dp(16));
        TextView fresh = Ui.button(c, "Yeni görsel", Ui.Style.PRIMARY, Icon.Kind.REFRESH);
        fresh.setOnClickListener(v -> { seed = rnd.nextLong(); nextHadith(); render(); });
        f.addView(fresh, Ui.lp(-1, c.dp(54)));

        LinearLayout row1 = new LinearLayout(c.a);
        TextView other = Ui.button(c, "Başka hadis", Ui.Style.TONAL, null);
        other.setOnClickListener(v -> { nextHadith(); render(); });
        orient = Ui.button(c, "Dikey 2:3", Ui.Style.TONAL, null);
        orient.setOnClickListener(v -> {
            portrait = !portrait;
            Ui.swapText(orient, portrait ? "Yatay 3:2" : "Dikey 2:3");
            render();
        });
        row1.addView(other, new LinearLayout.LayoutParams(0, c.dp(48), 1));
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(0, c.dp(48), 1);
        gap.setMarginStart(c.dp(10));
        row1.addView(orient, gap);
        f.addView(row1, Ui.lp(-1, -2, 0, c.dp(8), 0, 0));

        LinearLayout row2 = new LinearLayout(c.a);
        TextView save = Ui.button(c, "Kaydet", Ui.Style.TONAL, Icon.Kind.CHECK);
        save.setOnClickListener(v -> persist(false));
        TextView share = Ui.button(c, "Paylaş", Ui.Style.TONAL, Icon.Kind.SHARE);
        share.setOnClickListener(v -> persist(true));
        row2.addView(save, new LinearLayout.LayoutParams(0, c.dp(48), 1));
        LinearLayout.LayoutParams gap2 = new LinearLayout.LayoutParams(0, c.dp(48), 1);
        gap2.setMarginStart(c.dp(10));
        row2.addView(share, gap2);
        f.addView(row2, Ui.lp(-1, -2, 0, c.dp(8), 0, 0));

        TextView note = Ui.text(c, "Hadisler meâl olarak verilir; paylaşmadan önce kaynağını doğrulaman önerilir.", 12, c.p.muted, 400);
        note.setGravity(Gravity.CENTER);
        f.addView(note, Ui.lp(-1, -2, 0, c.dp(10), 0, 0));
        page.addView(f, new LinearLayout.LayoutParams(-1, -2));
        return page;
    }

    @Override public void onShown() { render(); }

    private void nextHadith() {
        int n = Hadiths.ALL.length, next = rnd.nextInt(n - 1);
        hadith = next >= hadith ? next + 1 : next;   // never the same twice in a row
    }

    private void render() {
        final int my = ++gen;
        final long s = seed; final boolean pt = portrait; final String[] h = Hadiths.ALL[hadith];
        c.io.execute(() -> {
            Bitmap b;
            try { b = FridayArt.render(s, pt, h[0], h[1]); }
            catch (Throwable t) { return; }
            c.ui.post(() -> {
                if (destroyed || my != gen) { b.recycle(); return; }
                Bitmap old = bmp;
                bmp = b; saved = null;
                preview.setImageBitmap(b);
                if (old != null) old.recycle();     // only after the view no longer shows it
            });
        });
    }

    /** Writes the card into Pictures/Kolay once (then reuses the Uri), optionally opening the share sheet. */
    private void persist(boolean share) {
        Bitmap b = bmp;
        if (b == null) return;
        if (saved != null) { done(saved, share, false); return; }
        c.io.execute(() -> {
            Uri out = null; String err = null;
            try {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Images.Media.DISPLAY_NAME, "Cuma_" + System.currentTimeMillis() + ".jpg");
                v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                v.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Kolay");
                v.put(MediaStore.Images.Media.IS_PENDING, 1);
                out = c.a.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                if (out == null) throw new Exception("Dosya oluşturulamadı");
                try (OutputStream os = c.a.getContentResolver().openOutputStream(out)) {
                    if (!b.compress(Bitmap.CompressFormat.JPEG, 94, os)) throw new Exception("Kaydedilemedi");
                }
                ContentValues d = new ContentValues();
                d.put(MediaStore.Images.Media.IS_PENDING, 0);
                c.a.getContentResolver().update(out, d, null, null);
            } catch (Throwable t) {
                if (out != null) try { c.a.getContentResolver().delete(out, null, null); } catch (Exception ignored) { }
                out = null; err = t.getMessage() == null ? "Kaydedilemedi" : t.getMessage();
            }
            final Uri u = out; final String e = err; final Bitmap target = b;
            c.ui.post(() -> {
                if (destroyed) return;
                if (u == null) { Toast.makeText(c.a, e, Toast.LENGTH_SHORT).show(); return; }
                if (bmp == target) saved = u;     // ignore if the card changed while saving
                done(u, share, true);
            });
        });
    }

    private void done(Uri u, boolean share, boolean fresh) {
        if (share) c.a.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("image/jpeg")
                .putExtra(Intent.EXTRA_STREAM, u).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null));
        else Toast.makeText(c.a, fresh ? "Pictures/Kolay klasörüne kaydedildi" : "Zaten kaydedildi", Toast.LENGTH_SHORT).show();
    }

    @Override public void onDestroy() {
        destroyed = true; gen++;
        if (preview != null) preview.setImageDrawable(null);
        if (bmp != null) { bmp.recycle(); bmp = null; }
    }
}
