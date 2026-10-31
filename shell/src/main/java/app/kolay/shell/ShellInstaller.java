package app.kolay.shell;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.provider.Settings;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Consumer;
import org.json.JSONObject;

/**
 * Downloads the newer shell APK named in the manifest and hands it to Android's installer.
 * Android always asks the user to confirm an update, so this cannot be silent; what it removes is the manual download.
 *
 * Checks before the installer is even involved: GitHub host + release URL, size cap, SHA-256, ECDSA signature with the
 * compiled-in update key, and the archive's package name, version code and signing certificate. Android re-checks the certificate itself.
 */
final class ShellInstaller {
    private ShellInstaller() {}

    static final String ACTION_RESULT = "app.kolay.shell.INSTALL_RESULT";

    /** @param progress receives short status texts on the main thread (the last one is final until the system dialog takes over). */
    static void run(Activity act, Consumer<String> progress) {
        Context app = act.getApplicationContext();
        if (!app.getPackageManager().canRequestPackageInstalls()) {
            act.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + app.getPackageName())));
            progress.accept("Önce Kolay için “Bu kaynaktan yüklemeye izin ver”i aç, sonra geri dönüp tekrar dokun.");
            return;
        }
        new Thread(() -> {
            File apk = new File(app.getCacheDir(), "shell.download");
            try {
                post(act, progress, "İndiriliyor…");
                JSONObject mf = new JSONObject(new String(Updater.fetch(Config.MANIFEST_URL, 64 << 10), StandardCharsets.UTF_8));
                if (!Updater.shellOffered(app, mf)) throw new IOException("yüklenecek yeni kabuk yok");
                long vc = mf.getLong("shellVersionCode"), size = mf.getLong("shellSize");
                Updater.download(mf.getString("shellUrl"), apk, size);

                post(act, progress, "Doğrulanıyor…");
                if (!Verify.sha256(apk).equals(mf.getString("shellSha256").toLowerCase())) throw new SecurityException("özet (SHA-256) uyuşmuyor");
                byte[] sig = Base64.getDecoder().decode(mf.getString("shellSignature"));
                if (!Verify.signature(apk, sig, Config.UPDATE_PUBLIC_KEYS)) throw new SecurityException("imza doğrulanamadı");
                checkArchive(app, apk, vc);

                post(act, progress, "Yükleme onayı bekleniyor…");
                install(app, apk, size);
            } catch (Exception e) {
                String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                post(act, progress, "Kabuk güncellenemedi: " + m);
            } finally { apk.delete(); }
        }, "shell-install").start();
    }

    private static void post(Activity a, Consumer<String> progress, String s) { a.runOnUiThread(() -> progress.accept(s)); }

    /** The file must be this app (same package, the announced version, same signing certificate). */
    private static void checkArchive(Context app, File apk, long vc) {
        PackageManager pm = app.getPackageManager();
        PackageInfo n = pm.getPackageArchiveInfo(apk.getAbsolutePath(), PackageManager.GET_SIGNING_CERTIFICATES);
        if (n == null || !app.getPackageName().equals(n.packageName)) throw new SecurityException("paket adı uyuşmuyor");
        if (n.getLongVersionCode() != vc) throw new SecurityException("sürüm numarası manifest ile uyuşmuyor");
        PackageInfo mine;
        try { mine = pm.getPackageInfo(app.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES); }
        catch (PackageManager.NameNotFoundException e) { throw new SecurityException("kendi imzam okunamadı"); }
        if (n.signingInfo == null || mine.signingInfo == null) throw new SecurityException("APK imzası okunamadı");
        for (Signature a : n.signingInfo.getApkContentsSigners())
            for (Signature b : mine.signingInfo.getApkContentsSigners())
                if (a.equals(b)) return;
        throw new SecurityException("APK imzası bu uygulamayla aynı değil");
    }

    private static void install(Context app, File apk, long size) throws IOException {
        PackageInstaller pi = app.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams sp = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        sp.setAppPackageName(app.getPackageName());
        sp.setSize(size);
        int id = pi.createSession(sp);
        try (PackageInstaller.Session s = pi.openSession(id)) {
            try (InputStream in = new FileInputStream(apk); OutputStream o = s.openWrite("kolay", 0, size)) {
                byte[] b = new byte[65536]; int n;
                while ((n = in.read(b)) > 0) o.write(b, 0, n);
                s.fsync(o);
            }
            Intent cb = new Intent(app, InstallResultReceiver.class).setAction(ACTION_RESULT).setPackage(app.getPackageName());
            // Mutable on purpose: the system fills in the status extras.
            PendingIntent pend = PendingIntent.getBroadcast(app, id, cb, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            s.commit(pend.getIntentSender());
        } catch (IOException | RuntimeException e) {
            try { pi.abandonSession(id); } catch (Exception ignored) { }
            throw e;
        }
    }
}
