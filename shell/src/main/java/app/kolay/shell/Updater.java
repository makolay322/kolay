package app.kolay.shell;

import android.content.Context;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.json.JSONObject;

/**
 * Checks GitHub Releases for a newer payload. Never touches the running payload: a verified download
 * is staged on disk and picked up at the next process start.
 *
 * Trust chain: manifest.json (hosted on GitHub) names an APK + SHA-256 + ECDSA signature. The signature
 * is checked against the public key compiled into the shell, so a hijacked repo cannot ship code.
 */
final class Updater {
    private Updater() {}

    /** Set by the host; called on the main thread after a new payload was staged. */
    static volatile Runnable onStaged;

    static void checkAsync(Context c, boolean force) {
        Thread t = new Thread(() -> check(c, force), "updater");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /** @return a short human-readable result, also stored for the settings screen. */
    static synchronized String check(Context ctx, boolean force) {
        Registry reg = new Registry(ctx);
        long now = System.currentTimeMillis();
        if (!force && now < reg.nextCheck()) return reg.status();
        String result;
        try {
            result = run(ctx, reg);
            reg.setNextCheck(now + Config.CHECK_INTERVAL_MS);
        } catch (Exception e) {
            result = "Denetim başarısız: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            reg.setNextCheck(now + Config.RETRY_AFTER_FAIL_MS);
        }
        reg.setStatus(result);
        return result;
    }

    private static String run(Context ctx, Registry reg) throws Exception {
        JSONObject mf = new JSONObject(new String(fetch(Config.MANIFEST_URL, 64 << 10), StandardCharsets.UTF_8));
        int vc = mf.getInt("versionCode");
        int api = mf.getInt("api");
        String url = mf.getString("url"), sha = mf.getString("sha256").toLowerCase(), sig = mf.getString("signature");
        long size = mf.getLong("size");
        String name = mf.optString("versionName", String.valueOf(vc));

        if (!url.startsWith(Config.REPO_BASE + "releases/download/")) throw new SecurityException("beklenmeyen indirme adresi");
        if (size <= 0 || size > Config.MAX_PAYLOAD_BYTES) throw new IOException("geçersiz boyut");
        if (api > Config.SHELL_API) {
            reg.setNeedsShellUpdate(true);
            return "Yeni sürüm (" + name + ") daha yeni bir kabuk gerektiriyor. Sürüm sayfasından kabuk APK'sını yükle.";
        }
        reg.setNeedsShellUpdate(false);
        if (api < Config.MIN_PAYLOAD_API) return "Son sürüm (" + name + ") bu kabukla uyumsuz, atlandı.";
        if (vc <= reg.highestKnown()) return "Güncel (modül v" + reg.highestKnown() + ")";

        File tmp = new File(ctx.getCacheDir(), "payload.download");
        tmp.delete();
        try {
            download(url, tmp, size);
            if (!Verify.sha256(tmp).equals(sha)) throw new SecurityException("özet (SHA-256) uyuşmuyor");
            byte[] sigBytes = Base64.getDecoder().decode(sig);
            if (!Verify.signature(tmp, sigBytes, Config.UPDATE_PUBLIC_KEYS)) throw new SecurityException("imza doğrulanamadı");

            Registry.Meta m = reg.readMeta(tmp);
            if (m == null || !Config.PAYLOAD_PKG.equals(m.pkg) || m.vc != vc || m.api != api)
                throw new SecurityException("modül bilgisi manifest ile uyuşmuyor");

            File dest = reg.fileFor(vc);
            File dir = dest.getParentFile();
            Registry.deleteTree(dir);
            dir.mkdirs();
            File sigFile = new File(dir, "payload.sig");
            try (OutputStream o = new FileOutputStream(sigFile)) { o.write(sigBytes); }
            if (!tmp.renameTo(dest)) copy(tmp, dest);
            dest.setReadOnly();  // Android 14+: loaded dex must be read-only
            reg.prune();
            Runnable r = onStaged;
            if (r != null) new android.os.Handler(android.os.Looper.getMainLooper()).post(r);
            return "Yeni sürüm hazır: " + name;
        } finally { tmp.delete(); }
    }

    private static HttpURLConnection open(String u) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(15000); c.setReadTimeout(30000);
        c.setRequestProperty("User-Agent", "kolay");
        c.setInstanceFollowRedirects(true);
        return c;
    }

    /** After redirects the final endpoint must still be GitHub over HTTPS. */
    private static void requireTrusted(HttpURLConnection c) throws IOException {
        URL u = c.getURL();
        if (!"https".equals(u.getProtocol()) || !Config.trustedHost(u.getHost())) throw new SecurityException("güvenilmeyen sunucu: " + u.getHost());
    }

    private static byte[] fetch(String u, int max) throws IOException {
        HttpURLConnection c = open(u);
        try {
            if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
            requireTrusted(c);
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream o = new ByteArrayOutputStream()) {
                byte[] b = new byte[8192]; int n;
                while ((n = in.read(b)) > 0) { o.write(b, 0, n); if (o.size() > max) throw new IOException("yanıt çok büyük"); }
                return o.toByteArray();
            }
        } finally { c.disconnect(); }
    }

    private static void download(String u, File to, long expected) throws IOException {
        HttpURLConnection c = open(u);
        try {
            if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
            requireTrusted(c);
            long total = 0;
            try (InputStream in = c.getInputStream(); OutputStream o = new FileOutputStream(to)) {
                byte[] b = new byte[65536]; int n;
                while ((n = in.read(b)) > 0) { total += n; if (total > expected) throw new IOException("dosya beklenenden büyük"); o.write(b, 0, n); }
            }
            if (total != expected) throw new IOException("indirme eksik (" + total + "/" + expected + ")");
        } finally { c.disconnect(); }
    }

    private static void copy(File a, File b) throws IOException {
        try (InputStream in = new FileInputStream(a); OutputStream o = new FileOutputStream(b)) {
            byte[] buf = new byte[65536]; int n; while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
        }
    }
}
