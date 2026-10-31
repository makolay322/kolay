package app.kolay.shell;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.RemoteViews;

/** Flat home-screen widget: the next prayer, its time and a live countdown. Tapping it opens the app. */
public class PrayerWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, views(c));
        Prayer.refresh(c);   // keeps the alarms armed even if the app was never opened after an install
    }

    @Override public void onEnabled(Context c) { Prayer.refresh(c); }

    static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, PrayerWidget.class));
        if (ids == null || ids.length == 0) return;
        RemoteViews v = views(c);
        for (int id : ids) m.updateAppWidget(id, v);
    }

    private static RemoteViews views(Context c) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_prayer);
        v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, 0,
            new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        long now = System.currentTimeMillis();
        long[] n = Prayer.next(c, now);
        if (n == null) {
            v.setTextViewText(R.id.label, "NAMAZ VAKİTLERİ");
            v.setTextViewText(R.id.time, "--:--");
            v.setViewVisibility(R.id.countdown, android.view.View.GONE);
            v.setViewVisibility(R.id.hint, android.view.View.VISIBLE);
            v.setTextViewText(R.id.hint, "Konum için dokun");
            return v;
        }
        v.setTextViewText(R.id.label, Prayer.NAMES[(int) n[0]].toUpperCase(java.util.Locale.forLanguageTag("tr")));
        v.setTextViewText(R.id.time, Prayer.clock(n[1]));
        v.setViewVisibility(R.id.hint, android.view.View.GONE);
        v.setViewVisibility(R.id.countdown, android.view.View.VISIBLE);
        v.setChronometerCountDown(R.id.countdown, true);
        v.setChronometer(R.id.countdown, SystemClock.elapsedRealtime() + (n[1] - now), "%s kaldı", true);
        return v;
    }
}
