package app.kolay.shell;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import app.kolay.app.Entry;
import java.io.*;
import java.util.function.Consumer;

/**
 * The only activity. Prepares the platform pieces (key store, OCR data, updater) and hands the window to the UI
 * ({@link Entry}). The class name stays {@code app.kolay.shell.MainActivity} on purpose: launcher shortcuts point at it.
 */
public class MainActivity extends Activity {
    private static final String TAG = "Kolay";
    /** True while the activity is resumed; the background updater installs only when this is false. */
    static volatile boolean foreground;

    private SecretStore secrets;
    private Entry ui;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        secrets = new SecretStore(this);
        Updater.cleanupLegacy(this);
        try { ensureTessdata(); } catch (IOException e) { Log.e(TAG, "tessdata", e); }
        ui = new Entry();
        ui.onCreate(new HostImpl(), saved);
        UpdateJob.schedule(this);
        Updater.checkAsync(this, false);
    }

    /** deu.traineddata ships in the APK's assets; Tesseract needs it as a plain file. */
    private void ensureTessdata() throws IOException {
        File dir = new File(getFilesDir(), "tessdata");
        File f = new File(dir, "deu.traineddata");
        long len;
        try (android.content.res.AssetFileDescriptor fd = getAssets().openFd("tessdata/deu.traineddata")) { len = fd.getLength(); }
        if (!f.exists() || f.length() != len) {
            dir.mkdirs();
            File tmp = new File(dir, "deu.tmp");
            try (InputStream in = getAssets().open("tessdata/deu.traineddata"); OutputStream o = new FileOutputStream(tmp)) {
                byte[] b = new byte[65536]; int n; while ((n = in.read(b)) > 0) o.write(b, 0, n);
            }
            f.delete();
            if (!tmp.renameTo(f)) throw new IOException("tessdata could not be placed");
        }
    }

    private final class HostImpl implements Host {
        @Override public Activity activity() { return MainActivity.this; }
        @Override public String secret(String n) { return secrets.get(n); }
        @Override public void setSecret(String n, String v) { secrets.set(n, v); }
        @Override public File tessDataParent() { return getFilesDir(); }
        @Override public String versionName() { return Updater.installedName(MainActivity.this); }
        @Override public String updateStatus() { return Updater.status(MainActivity.this); }
        @Override public boolean updateReady() { return Updater.ready(MainActivity.this); }
        @Override public String updateName() { return Updater.stagedName(MainActivity.this); }
        @Override public void checkForUpdates(Consumer<String> done) {
            new Thread(() -> {
                String s = Updater.check(getApplicationContext(), true);
                runOnUiThread(() -> done.accept(s));
            }, "update-check").start();
        }
        @Override public void onUpdateReady(Runnable r) { Updater.onReady = r; }
        @Override public void installUpdate(Consumer<String> progress) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));
                progress.accept("Önce Kolay için “Bu kaynaktan yüklemeye izin ver”i aç, sonra geri dönüp tekrar dokun.");
                return;
            }
            progress.accept("Kuruluyor… Uygulama birazdan kapanır; yeniden açınca yeni sürüm çalışır.");
            new Thread(() -> {
                try { Updater.install(getApplicationContext(), false); }
                catch (Exception e) {
                    String m = "Güncelleme kurulamadı: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                    runOnUiThread(() -> progress.accept(m));
                }
            }, "update-install").start();
        }
        @Override public String releasesUrl() { return Config.RELEASES_PAGE; }
        @Override public double[] prayerLocation() { return Prayer.location(MainActivity.this); }
        @Override public void prayerSetLocation(double lat, double lon) { Prayer.setLocation(MainActivity.this, lat, lon); }
        @Override public long[] prayerTimes(int y, int m, int d) { return Prayer.times(MainActivity.this, y, m, d); }
        @Override public int prayerReminderMask() { return Prayer.reminderMask(MainActivity.this); }
        @Override public void prayerSetReminderMask(int mask) { Prayer.setReminderMask(MainActivity.this, mask); }
        @Override public boolean prayerPinWidget() { return Prayer.pinWidget(MainActivity.this); }
    }

    // ---- lifecycle forwarding ------------------------------------------------------------------

    @Override protected void onResume() { super.onResume(); foreground = true; ui.onResume(); }

    @Override protected void onPause() { foreground = false; ui.onPause(); super.onPause(); }

    @Override public void onRequestPermissionsResult(int req, String[] p, int[] r) {
        super.onRequestPermissionsResult(req, p, r);
        ui.onPermissionResult(req, p, r);
    }

    @Override public void onBackPressed() { if (!ui.onBackPressed()) super.onBackPressed(); }

    @Override protected void onDestroy() {
        Updater.onReady = null;
        ui.onDestroy();
        super.onDestroy();
    }
}
