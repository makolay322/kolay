package app.kolay.shell;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

/**
 * Installer progress for a self-update. A confirmation request is shown at once when the app is on screen; from the
 * background job it becomes a notification instead (Android does not let a background app open screens). Not exported.
 */
public class InstallResultReceiver extends BroadcastReceiver {
    private static final String CHANNEL = "updates";

    @Override public void onReceive(Context ctx, Intent i) {
        int status = i.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            if (i.getBooleanExtra(Updater.EXTRA_BACKGROUND, false) && !MainActivity.foreground) { notifyReady(ctx); return; }
            Intent confirm = android.os.Build.VERSION.SDK_INT >= 33
                ? i.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class) : i.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirm != null) ctx.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (status != PackageInstaller.STATUS_SUCCESS) {
            String m = i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            Updater.setStatus(ctx, status == PackageInstaller.STATUS_FAILURE_ABORTED ? "Güncelleme iptal edildi; ana sayfadaki banttan tekrar kurabilirsin."
                : "Güncelleme kurulamadı" + (m == null ? "." : ": " + m));
        }
    }

    /** "Yeni sürüm hazır" notification: tapping opens Kolay, whose home-screen banner installs it. */
    private static void notifyReady(Context ctx) {
        try {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm == null) return;
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Uygulama güncellemesi", NotificationManager.IMPORTANCE_DEFAULT));
            Intent open = new Intent(ctx, MainActivity.class).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(ctx, 7, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            String name = Updater.stagedName(ctx);
            nm.notify(7, new Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_mono)
                .setContentTitle("Kolay güncellemesi hazır" + (name.isEmpty() ? "" : " (" + name + ")"))
                .setContentText("Kurmak için dokun.")
                .setContentIntent(pi).setAutoCancel(true).build());
        } catch (Exception ignored) { }   // notifications off: the banner still shows on the next start
    }
}
