package app.kolay.shell;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.*;
import android.widget.RemoteViews;

/**
 * Home-screen widget: only the next prayer's time, 24-hour, as pastel-gradient light text with no background. It adapts to its
 * size: one line when wide, hours above minutes otherwise, always scaled to fill the widget. Tapping it opens the app.
 */
public class PrayerWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) paint(c, m, id);
        Prayer.refresh(c);   // keeps the alarms armed even if the app was never opened after an install
    }

    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle options) { paint(c, m, id); }

    @Override public void onEnabled(Context c) { Prayer.refresh(c); }

    static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, PrayerWidget.class));
        if (ids == null) return;
        for (int id : ids) paint(c, m, id);
    }

    private static void paint(Context c, AppWidgetManager m, int id) {
        Bundle o = m.getAppWidgetOptions(id);
        // size in dp as the launcher currently shows it (min width / max height of the portrait case)
        float w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110), h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 110);
        if (w <= 0) w = 110;
        if (h <= 0) h = 110;
        // Stack hours over minutes unless the widget is clearly wide: a narrow widget then gets big digits instead of tiny ones.
        boolean stacked = h >= w * 0.8f;

        long[] n = Prayer.next(c, System.currentTimeMillis());
        String t = n == null ? "--:--" : Prayer.clock(n[1]);

        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_prayer);
        v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, 0,
            new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        v.setImageViewBitmap(R.id.img, draw(c, t, w, h, stacked));
        v.setContentDescription(R.id.root, n == null ? "Namaz vakitleri" : "Sıradaki namaz vakti " + t);
        m.updateAppWidget(id, v);
    }

    /** Pastel-gradient time on a transparent bitmap, text scaled to fill the widget (bitmap kept small: RemoteViews has a size limit). */
    private static Bitmap draw(Context c, String t, float wDp, float hDp, boolean stacked) {
        float d = c.getResources().getDisplayMetrics().density;
        float scale = Math.min(1f, 640f / Math.max(wDp * d, hDp * d));
        int bw = Math.max(32, Math.round(wDp * d * scale)), bh = Math.max(32, Math.round(hDp * d * scale));
        Bitmap bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        Typeface base = Typeface.create("sans-serif", Typeface.NORMAL);
        p.setTypeface(Typeface.create(base, 300, false));     // light weight (API 28+, minSdk is 29)
        p.setTextAlign(Paint.Align.CENTER);
        p.setLetterSpacing(stacked ? 0f : 0.02f);

        String[] lines = stacked && t.length() == 5 ? new String[]{t.substring(0, 2), t.substring(3)} : new String[]{t};
        float pad = Math.min(bw, bh) * 0.04f;
        float availW = bw - 2 * pad, availH = bh - 2 * pad;
        // fit: measure at a reference size, then scale to the tighter of width and height
        p.setTextSize(100f);
        float maxW = 0;
        for (String l : lines) maxW = Math.max(maxW, p.measureText(l));
        Paint.FontMetrics fm = p.getFontMetrics();
        float digitH = -fm.ascent * 0.74f;                    // cap/digit height is about 74% of the ascent
        float gap = 28f;                                       // reference px between stacked lines
        float totalH = lines.length * digitH + (lines.length - 1) * gap;
        float k = Math.min(availW / Math.max(1f, maxW), availH / Math.max(1f, totalH));
        p.setTextSize(100f * k);
        digitH *= k; gap *= k;
        float blockH = lines.length * digitH + (lines.length - 1) * gap;

        // pastel diagonal gradient across the block (lavender -> pink -> peach), soft shadow keeps it readable on light wallpapers
        p.setShader(new LinearGradient(pad, pad, bw - pad, bh - pad,
            new int[]{0xFFB4C0FF, 0xFFF5B8E4, 0xFFFFD0A8}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        p.setShadowLayer(Math.max(2f, k * 6f), 0, k * 2f, 0x66000000);
        float y = (bh - blockH) / 2f + digitH;
        for (String l : lines) { cv.drawText(l, bw / 2f, y, p); y += digitH + gap; }
        return bmp;
    }
}
