package app.kolay.payload.tools.friday;

import android.graphics.*;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import java.util.Random;

/**
 * Draws the Friday card entirely with Canvas (no image assets): a gradient, a geometric Islamic pattern, a crescent,
 * stars and a plain mosque silhouette (no window lights), with the hadith in a translucent panel. The same seed always
 * gives the same background. Only dark themes exist: half near-black, half deep mature colors.
 */
final class FridayArt {
    private FridayArt() {}

    private static final class Theme {
        final int top, bottom, accent, text, panel, mosque;
        Theme(int top, int bottom, int accent, int text, int panel, int mosque) {
            this.top = lift(top); this.bottom = lift(bottom); this.accent = accent; this.text = text; this.panel = panel; this.mosque = mosque;
        }
    }

    /** Brightens a background color (x1.7 + 16 per channel) while keeping its hue, so the cards are dark but not murky. */
    private static int lift(int c) {
        int r = Math.min(255, Math.round(((c >> 16) & 255) * 1.7f) + 16), g = Math.min(255, Math.round(((c >> 8) & 255) * 1.7f) + 16), b = Math.min(255, Math.round((c & 255) * 1.7f) + 16);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static final Theme[] DARK = {
        new Theme(0xFF030607, 0xFF0A1F1E, 0xFFD4AF37, 0xFFF5F1E6, 0x66000000, 0xFF020807),
        new Theme(0xFF05050A, 0xFF1B1038, 0xFFE8C872, 0xFFF3EEFF, 0x66000000, 0xFF020208),
        new Theme(0xFF000000, 0xFF10202B, 0xFF7FD8E8, 0xFFEAF6F8, 0x66000000, 0xFF03070A),
        new Theme(0xFF0B0B0B, 0xFF241A0A, 0xFFFFC857, 0xFFFFF4DC, 0x66000000, 0xFF050300),
    };
    /** Deep, mature colors (never bright): emerald, navy, burgundy, plum, olive. */
    private static final Theme[] MATURE = {
        new Theme(0xFF0B3D2E, 0xFF04201A, 0xFFD9B44A, 0xFFF4EFE0, 0x66000000, 0xFF021410),
        new Theme(0xFF0E2340, 0xFF050E1F, 0xFFC9A85C, 0xFFEEF2F8, 0x66000000, 0xFF030812),
        new Theme(0xFF4A1523, 0xFF1F0912, 0xFFE0B96B, 0xFFF8EEE8, 0x66000000, 0xFF140509),
        new Theme(0xFF2E1A47, 0xFF130A21, 0xFFD8B86A, 0xFFF1EAF7, 0x66000000, 0xFF0B0615),
        new Theme(0xFF3B3A1F, 0xFF1A190B, 0xFFD6B25E, 0xFFF6F1DD, 0x66000000, 0xFF0F0F05),
    };

    /** @return a new square (1:1) bitmap; the caller owns it */
    static Bitmap render(long seed, String hadith, String source) {
        final int w = 1200, h = 1200;
        final float u = 1f;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        Random r = new Random(seed);
        boolean dark = r.nextBoolean();
        Theme t = dark ? DARK[r.nextInt(DARK.length)] : MATURE[r.nextInt(MATURE.length)];

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, 0, h, t.top, t.bottom, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, p);
        p.setShader(null);

        pattern(cv, w, h, t, r);
        moon(cv, w, h, t, r);
        sparkles(cv, w, h, t, r);
        mosque(cv, w, h, t, r);
        panelAndText(cv, w, h, u, t, hadith, source);
        return bmp;
    }

    // ---- background ---------------------------------------------------------------------------

    private static void pattern(Canvas cv, int w, int h, Theme t, Random r) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, w / 520f));
        p.setColor(t.accent);
        p.setAlpha(34);
        float s = w / (7f + r.nextInt(5));
        boolean stars = r.nextBoolean();
        for (float y = -s / 2; y < h + s; y += s) {
            for (float x = -s / 2; x < w + s; x += s) {
                if (stars) {
                    float d = s * 0.40f;
                    cv.drawRect(x - d, y - d, x + d, y + d, p);
                    cv.save(); cv.rotate(45, x, y); cv.drawRect(x - d, y - d, x + d, y + d, p); cv.restore();
                } else {
                    cv.drawCircle(x, y, s * 0.5f, p);
                    cv.drawCircle(x + s / 2, y + s / 2, s * 0.5f, p);
                }
            }
        }
    }

    private static void moon(Canvas cv, int w, int h, Theme t, Random r) {
        boolean left = r.nextBoolean();
        float rad = Math.min(w, h) * 0.085f;
        float cx = left ? w * 0.13f : w * 0.87f, cy = h * (0.12f + r.nextFloat() * 0.03f);
        float dir = left ? 1 : -1;
        Paint g = new Paint(Paint.ANTI_ALIAS_FLAG);
        g.setShader(new RadialGradient(cx, cy, rad * 3f, (t.accent & 0x00FFFFFF) | 0x38000000, t.accent & 0x00FFFFFF, Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, rad * 3f, g);
        Path outer = new Path(), inner = new Path();
        outer.addCircle(cx, cy, rad, Path.Direction.CW);
        inner.addCircle(cx + dir * rad * 0.42f, cy - rad * 0.12f, rad * 0.82f, Path.Direction.CW);
        outer.op(inner, Path.Op.DIFFERENCE);
        Paint f = new Paint(Paint.ANTI_ALIAS_FLAG);
        f.setColor(t.accent);
        cv.drawPath(outer, f);
        cv.drawPath(star(cx + dir * rad * 0.95f, cy - rad * 0.05f, rad * 0.30f, rad * 0.13f, 5), f);
    }

    private static void sparkles(Canvas cv, int w, int h, Theme t, Random r) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int n = 22 + r.nextInt(18);
        for (int i = 0; i < n; i++) {
            float x = r.nextFloat() * w, y = r.nextFloat() * h * 0.62f, s = 2f + r.nextFloat() * 5f * Math.min(w, h) / 1200f;
            p.setColor(t.accent);
            p.setAlpha(90 + r.nextInt(150));
            if (r.nextInt(3) == 0) cv.drawPath(star(x, y, s * 2.4f, s * 0.7f, 4), p);
            else cv.drawCircle(x, y, s, p);
        }
    }

    /** Domes and minarets along the bottom edge. */
    private static void mosque(Canvas cv, int w, int h, Theme t, Random r) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(t.mosque);
        float cx = w * (0.38f + r.nextFloat() * 0.24f);
        float mw = w * 0.40f;
        float bodyTop = h * 0.905f;
        float dr = mw * 0.21f;

        cv.drawRect(0, h * 0.965f, w, h, p);                       // ground
        cv.drawRect(cx - mw / 2, bodyTop, cx + mw / 2, h, p);      // prayer hall
        dome(cv, p, cx, bodyTop, dr, dr * 1.15f);                  // main dome
        dome(cv, p, cx - mw * 0.36f, bodyTop + h * 0.012f, dr * 0.55f, dr * 0.65f);
        dome(cv, p, cx + mw * 0.36f, bodyTop + h * 0.012f, dr * 0.55f, dr * 0.65f);
        cv.drawRect(cx - 2f, bodyTop - dr * 1.15f - h * 0.035f, cx + 2f, bodyTop - dr * 1.1f, p);   // finial
        cv.drawCircle(cx, bodyTop - dr * 1.15f - h * 0.035f, h * 0.007f, p);

        int pairs = r.nextBoolean() ? 1 : 2;
        for (int i = 0; i < pairs; i++) {
            float off = mw / 2 + w * (0.04f + 0.13f * i);
            float tip = h * (0.745f + 0.03f * r.nextFloat());
            minaret(cv, p, cx - off, tip, h, w);
            minaret(cv, p, cx + off, tip + h * 0.01f * (i + 1), h, w);
        }
    }

    private static void dome(Canvas cv, Paint p, float cx, float baseY, float rx, float ry) {
        cv.drawArc(new RectF(cx - rx, baseY - ry, cx + rx, baseY + ry), 180, 180, true, p);
    }

    private static void minaret(Canvas cv, Paint p, float x, float tipY, int h, int w) {
        float half = w * 0.009f, cone = h * 0.05f;
        float shaftTop = tipY + cone;
        cv.drawRect(x - half, shaftTop, x + half, h, p);
        cv.drawRect(x - half * 2f, shaftTop + h * 0.07f, x + half * 2f, shaftTop + h * 0.082f, p);   // balcony
        Path c = new Path();
        c.moveTo(x - half * 1.5f, shaftTop); c.lineTo(x, tipY); c.lineTo(x + half * 1.5f, shaftTop); c.close();
        cv.drawPath(c, p);
        cv.drawCircle(x, tipY - h * 0.006f, h * 0.0045f, p);
    }

    // ---- text ---------------------------------------------------------------------------------

    private static void panelAndText(Canvas cv, int w, int h, float u, Theme t, String hadith, String source) {
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setTextAlign(Paint.Align.CENTER);

        tp.setColor(t.text);
        tp.setTextSize(76 * u);
        tp.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        cv.drawText("Cumamız Mübarek Olsun", w / 2f, h * 0.175f, tp);

        float pl = w * 0.07f, pr = w * 0.93f, pt = h * 0.26f, pb = h * 0.76f;
        Paint pp = new Paint(Paint.ANTI_ALIAS_FLAG);
        pp.setColor(t.panel);
        float rad = 28 * u;
        cv.drawRoundRect(new RectF(pl, pt, pr, pb), rad, rad, pp);
        pp.setStyle(Paint.Style.STROKE); pp.setStrokeWidth(3 * u); pp.setColor(t.accent); pp.setAlpha(150);
        cv.drawRoundRect(new RectF(pl, pt, pr, pb), rad, rad, pp);

        float pad = 44 * u;
        float inner = (pr - pl) - 2 * pad;

        tp.setTypeface(Typeface.DEFAULT_BOLD);
        tp.setColor(t.accent);
        tp.setTextSize(26 * u);
        tp.setLetterSpacing(0.18f);
        float labelY = pt + pad + 20 * u;
        cv.drawText("HADÎS-İ ŞERÎF", w / 2f, labelY, tp);
        tp.setLetterSpacing(0f);

        // source line sits at the bottom of the panel; the hadith is fitted in between
        tp.setTypeface(Typeface.create(Typeface.SERIF, Typeface.ITALIC));
        tp.setTextSize(34 * u);
        float srcY = pb - pad * 0.7f;
        cv.drawText("— " + source, w / 2f, srcY, tp);

        float top = labelY + 26 * u, bottom = srcY - 58 * u;
        TextPaint hp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        hp.setColor(t.text);
        hp.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
        String body = "“" + hadith + "”";
        StaticLayout lay = null;
        for (float size = 66 * u; size >= 26 * u; size -= 2 * u) {
            hp.setTextSize(size);
            lay = StaticLayout.Builder.obtain(body, 0, body.length(), hp, Math.round(inner))
                    .setAlignment(Layout.Alignment.ALIGN_CENTER).setLineSpacing(0, 1.18f).build();
            if (lay.getHeight() <= bottom - top) break;
        }
        cv.save();
        cv.translate(pl + pad, top + Math.max(0, (bottom - top - lay.getHeight()) / 2f));
        lay.draw(cv);
        cv.restore();
    }

    // ---- shapes -------------------------------------------------------------------------------

    private static Path star(float cx, float cy, float outer, float innerR, int points) {
        Path s = new Path();
        for (int i = 0; i < points * 2; i++) {
            double a = -Math.PI / 2 + i * Math.PI / points;
            float rr = (i % 2 == 0) ? outer : innerR;
            float x = cx + (float) Math.cos(a) * rr, y = cy + (float) Math.sin(a) * rr;
            if (i == 0) s.moveTo(x, y); else s.lineTo(x, y);
        }
        s.close();
        return s;
    }
}
