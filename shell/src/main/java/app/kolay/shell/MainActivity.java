package app.kolay.shell;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.*;
import app.kolay.sdk.Host;
import app.kolay.sdk.Module;
import java.io.*;
import java.util.function.Consumer;

/**
 * The stable shell. It owns nothing but: picking the best healthy payload, loading it, forwarding
 * lifecycle calls to it, and keeping it updated. If a payload misbehaves it is dropped and the next
 * older one (ultimately the bundled one) takes over, so a bad release can never brick the app.
 */
public class MainActivity extends Activity {
    private static final String TAG = "Kolay";
    private Registry reg;
    private SecretStore secrets;
    private Module module;
    private boolean resumed;
    /** How long a launcher start keeps the animated splash on screen (the icon animation runs ~2.2 s). */
    private static final long SPLASH_MS = 2400;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        reg = new Registry(this);
        secrets = new SecretStore(this);
        if (reg.secure()) reg.setSecure(false);   // the screenshot-block setting was removed; clear a stored "on"
        applySecure(false);
        installCrashHandler();
        if (saved == null && Build.VERSION.SDK_INT >= 31 && getIntent() != null && Intent.ACTION_MAIN.equals(getIntent().getAction())
                && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)) holdSplash(() -> boot(saved));
        else boot(saved);
        UpdateJob.schedule(this);
        Updater.checkAsync(this, false);
    }

    /**
     * Keeps the system splash (and its animated icon) up for {@link #SPLASH_MS}: the first frame is withheld until then, and the
     * payload is started just before the end so its own entrance animations play visibly. Only for launcher starts, so the
     * QR tile and the share target open without delay.
     */
    private void holdSplash(Runnable start) {
        final long until = SystemClock.uptimeMillis() + SPLASH_MS;
        final View d = getWindow().getDecorView();
        final boolean[] started = {false};
        d.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                if (!started[0] || SystemClock.uptimeMillis() < until) return false;
                if (d.getViewTreeObserver().isAlive()) d.getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        });
        Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            start.run();
            started[0] = true;
            if (resumed && module != null) guard(module::onResume);   // the activity resumed before the payload existed
            d.invalidate();
        }, SPLASH_MS - 250);
        h.postDelayed(d::invalidate, SPLASH_MS + 20);
    }

    private void applySecure(boolean on) {
        if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    private void installCrashHandler() {
        Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try { reg.markCrash(); } catch (Throwable ignored) { }
            if (prev != null) prev.uncaughtException(t, e);
        });
    }

    private void boot(Bundle saved) {
        Throwable last = null;
        try {
            reg.ensureBundled();
            reg.evaluatePreviousSession();
            ensureTessdata();
        } catch (Throwable t) { last = t; Log.e(TAG, "prepare failed", t); }

        for (Registry.Cand c : reg.candidates()) {
            try {
                Module m = Loader.load(this, reg, c);
                Registry.Meta meta = reg.readMeta(c.apk);
                reg.startSession(c.vc);
                reg.setRunningName(meta == null ? "" : meta.name);
                m.onCreate(new HostImpl(c.vc, meta == null ? "" : meta.name), saved);
                module = m;
                return;
            } catch (Throwable t) {
                last = t;
                Log.e(TAG, "payload v" + c.vc + " rejected", t);
                reg.clearSession();
                reg.blacklist(c.vc);  // no-op for the bundled payload
            }
        }
        showFatal(last);
    }

    /** Last resort when not even the bundled payload loads: explain and point at the shell download. */
    private void showFatal(Throwable t) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL); l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(64, 64, 64, 64);
        TextView h = new TextView(this); h.setText("Kolay açılamadı"); h.setTextSize(24); h.setTypeface(Typeface.DEFAULT_BOLD);
        TextView b = new TextView(this); b.setPadding(0, 24, 0, 24); b.setTextSize(14);
        b.setText("Uygulama modülü yüklenemedi. Kabuk APK'sının son sürümünü yüklemek sorunu çözer.\n\n" + (t == null ? "" : t.getClass().getSimpleName() + ": " + t.getMessage()));
        Button open = new Button(this); open.setText("Sürüm sayfasını aç");
        open.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Config.RELEASES_PAGE))));
        l.addView(h); l.addView(b); l.addView(open);
        setContentView(l);
    }

    /** deu.traineddata lives in the shell (it is large and rarely changes); payloads receive its location via Host. */
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

    private int shellVersion() {
        try { PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0); return (int) pi.getLongVersionCode(); }
        catch (Exception e) { return 0; }
    }

    private final class HostImpl implements Host {
        private final int vc; private final String name;
        HostImpl(int vc, String name) { this.vc = vc; this.name = name; }
        @Override public Activity activity() { return MainActivity.this; }
        @Override public String secret(String n) { return secrets.get(n); }
        @Override public void setSecret(String n, String v) { secrets.set(n, v); }
        @Override public File tessDataParent() { return getFilesDir(); }
        @Override public void ready() { reg.markReady(); }
        @Override public int payloadVersion() { return vc; }
        @Override public String payloadName() { return name; }
        @Override public int shellVersion() { return MainActivity.this.shellVersion(); }
        @Override public boolean secureWindow() { return reg.secure(); }
        @Override public void setSecureWindow(boolean on) { reg.setSecure(on); applySecure(on); }
        @Override public String updateStatus() { return reg.status(); }
        @Override public boolean updateReady() { return reg.updateReady(); }
        @Override public boolean shellUpdateNeeded() { return reg.needsShellUpdate(); }
        @Override public void checkForUpdates(Consumer<String> done) {
            new Thread(() -> {
                String s = Updater.check(getApplicationContext(), true);
                runOnUiThread(() -> done.accept(s));
            }, "update-check").start();
        }
        @Override public boolean shellUpdateAvailable() { return reg.shellApkAvailable(); }
        @Override public void installShellUpdate(Consumer<String> progress) { ShellInstaller.run(MainActivity.this, progress); }
        @Override public void onUpdateReady(Runnable r) { Updater.onStaged = r; }
        @Override public void restart() { recreate(); }
        @Override public String releasesUrl() { return Config.RELEASES_PAGE; }
        @Override public double[] prayerLocation() { return Prayer.location(MainActivity.this); }
        @Override public void prayerSetLocation(double lat, double lon) { Prayer.setLocation(MainActivity.this, lat, lon); }
        @Override public long[] prayerTimes(int y, int m, int d) { return Prayer.times(MainActivity.this, y, m, d); }
        @Override public boolean prayerReminder() { return Prayer.reminder(MainActivity.this); }
        @Override public void prayerSetReminder(boolean on) { Prayer.setReminder(MainActivity.this, on); }
        @Override public int prayerReminderMask() { return Prayer.reminderMask(MainActivity.this); }
        @Override public void prayerSetReminderMask(int mask) { Prayer.setReminderMask(MainActivity.this, mask); }
        @Override public boolean prayerPinWidget() { return Prayer.pinWidget(MainActivity.this); }
    }

    // ---- lifecycle forwarding: a payload fault must never take the shell down ------------------

    @Override protected void onResume() { super.onResume(); resumed = true; guard(() -> { if (module != null) module.onResume(); }); }

    @Override protected void onPause() { resumed = false; guard(() -> { if (module != null) module.onPause(); }); super.onPause(); }

    @Override public void onRequestPermissionsResult(int req, String[] p, int[] r) {
        super.onRequestPermissionsResult(req, p, r);
        guard(() -> { if (module != null) module.onPermissionResult(req, p, r); });
    }

    @Override public void onBackPressed() {
        boolean[] used = {false};
        guard(() -> { if (module != null) used[0] = module.onBackPressed(); });
        if (!used[0]) super.onBackPressed();
    }

    @Override protected void onDestroy() {
        Updater.onStaged = null;
        guard(() -> { if (module != null) module.onDestroy(); });
        super.onDestroy();
    }

    private void guard(Runnable r) {
        try { r.run(); } catch (Throwable t) { Log.e(TAG, "payload fault", t); reg.markCrash(); }
    }
}
