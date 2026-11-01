package app.kolay.shell;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.RemoteViews;

/**
 * Home-screen widget: only the next prayer's time, 24-hour. It adapts to its size: a single line when wide or square,
 * hours above minutes when tall, and the text size follows the widget size. Tapping it opens the app.
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
        // width/height in dp as the launcher currently shows it (portrait min width, landscape min height are the safe values)
        float w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110), h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 110);
        if (w <= 0) w = 110;
        if (h <= 0) h = 110;
        boolean tall = h > w * 1.25f;

        long now = System.currentTimeMillis();
        long[] n = Prayer.next(c, now);
        String t = n == null ? "--:--" : Prayer.clock(n[1]);

        RemoteViews v = new RemoteViews(c.getPackageName(), tall ? R.layout.widget_prayer_tall : R.layout.widget_prayer);
        v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, 0,
            new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        if (tall) {
            float size = clamp(Math.min(w * 0.62f, h * 0.36f), 16, 96);
            v.setTextViewText(R.id.hh, t.length() == 5 ? t.substring(0, 2) : "--");
            v.setTextViewText(R.id.mm, t.length() == 5 ? t.substring(3) : "--");
            v.setTextViewTextSize(R.id.hh, TypedValue.COMPLEX_UNIT_DIP, size);
            v.setTextViewTextSize(R.id.mm, TypedValue.COMPLEX_UNIT_DIP, size);
        } else {
            float size = clamp(Math.min(h * 0.58f, w * 0.30f), 14, 96);
            v.setTextViewText(R.id.time, t);
            v.setTextViewTextSize(R.id.time, TypedValue.COMPLEX_UNIT_DIP, size);
        }
        v.setContentDescription(R.id.root, n == null ? "Namaz vakitleri" : "Sıradaki namaz vakti " + t);
        m.updateAppWidget(id, v);
    }

    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
}
