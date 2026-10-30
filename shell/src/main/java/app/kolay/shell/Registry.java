package app.kolay.shell;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import java.io.*;
import java.util.*;

/**
 * Bookkeeping for payload versions: which exist on disk, which are blacklisted, and whether the
 * previous session was healthy. A payload is only ever ACTIVATED at the next process start.
 *
 * Layout: files/payloads/&lt;versionCode&gt;/payload.apk (+ payload.sig for downloaded ones), read-only.
 */
final class Registry {
    static final class Cand {
        final int vc; final File apk; final File sig; final boolean bundled;
        Cand(int vc, File apk, File sig, boolean bundled) { this.vc = vc; this.apk = apk; this.sig = sig; this.bundled = bundled; }
    }

    /** Parsed from the APK's manifest without loading it. */
    static final class Meta { String pkg; String name; int vc; int api; }

    private static final int BLACKLIST_AFTER_BAD_SESSIONS = 2;

    private final Context ctx;
    private final SharedPreferences p;

    Registry(Context c) { ctx = c.getApplicationContext(); p = ctx.getSharedPreferences("registry", Context.MODE_PRIVATE); }

    File dir() { File d = new File(ctx.getFilesDir(), "payloads"); d.mkdirs(); return d; }
    File fileFor(int vc) { return new File(new File(dir(), String.valueOf(vc)), "payload.apk"); }

    // ---- versions ---------------------------------------------------------------------------

    int bundledVc() { return p.getInt("bundled_vc", 0); }

    /** Copies assets/payload.apk to disk (once per bundled version) so there is always a working fallback. */
    void ensureBundled() throws IOException {
        Properties pr = new Properties();
        try (InputStream in = ctx.getAssets().open("payload.properties")) { pr.load(in); }
        int vc = Integer.parseInt(pr.getProperty("versionCode").trim());
        File f = fileFor(vc);
        long len;
        try (android.content.res.AssetFileDescriptor fd = ctx.getAssets().openFd("payload.apk")) { len = fd.getLength(); }
        if (!f.exists() || f.length() != len) {
            f.getParentFile().mkdirs();
            File tmp = new File(f.getParentFile(), "payload.tmp");
            tmp.delete();
            try (InputStream in = ctx.getAssets().open("payload.apk"); OutputStream o = new FileOutputStream(tmp)) {
                byte[] b = new byte[65536]; int n; while ((n = in.read(b)) > 0) o.write(b, 0, n);
            }
            tmp.setReadOnly();  // Android 14+: dex files must not be writable
            f.delete();
            if (!tmp.renameTo(f)) throw new IOException("bundled payload could not be placed");
        }
        p.edit().putInt("bundled_vc", vc).apply();
    }

    /** Usable payloads, newest first: downloaded ones that are not blacklisted, then the bundled one. */
    List<Cand> candidates() {
        Set<String> bad = p.getStringSet("bad", Collections.emptySet());
        List<Cand> out = new ArrayList<>();
        File[] kids = dir().listFiles();
        if (kids != null) for (File k : kids) {
            int vc;
            try { vc = Integer.parseInt(k.getName()); } catch (NumberFormatException e) { continue; }
            File apk = new File(k, "payload.apk");
            if (!apk.isFile() || bad.contains(String.valueOf(vc))) continue;
            out.add(new Cand(vc, apk, new File(k, "payload.sig"), vc == bundledVc()));
        }
        out.sort((a, b) -> Integer.compare(b.vc, a.vc));
        return out;
    }

    /** Highest version we must not re-download: anything on disk or blacklisted, plus the bundled one. */
    int highestKnown() {
        int m = bundledVc();
        for (String s : p.getStringSet("bad", Collections.emptySet())) try { m = Math.max(m, Integer.parseInt(s)); } catch (NumberFormatException ignored) { }
        File[] kids = dir().listFiles();
        if (kids != null) for (File k : kids) try { m = Math.max(m, Integer.parseInt(k.getName())); } catch (NumberFormatException ignored) { }
        return m;
    }

    void blacklist(int vc) {
        if (vc == bundledVc()) return;  // the bundled payload is the last resort and is never dropped
        Set<String> bad = new HashSet<>(p.getStringSet("bad", Collections.emptySet()));
        bad.add(String.valueOf(vc));
        p.edit().putStringSet("bad", bad).commit();
    }

    /** Keeps the bundled payload plus the two newest others; the rest is deleted. */
    void prune() {
        List<Cand> all = new ArrayList<>();
        File[] kids = dir().listFiles();
        if (kids == null) return;
        for (File k : kids) try { int vc = Integer.parseInt(k.getName()); all.add(new Cand(vc, null, null, vc == bundledVc())); } catch (NumberFormatException ignored) { }
        all.sort((a, b) -> Integer.compare(b.vc, a.vc));
        int kept = 0;
        for (Cand c : all) {
            if (c.bundled) continue;
            if (++kept > 2) deleteTree(new File(dir(), String.valueOf(c.vc)));
        }
    }

    static void deleteTree(File f) {
        File[] k = f.listFiles();
        if (k != null) for (File c : k) deleteTree(c);
        f.setWritable(true); f.delete();
    }

    // ---- session health ---------------------------------------------------------------------

    /** Judges the previous run: it must have reported ready and not crashed. Two bad runs in a row blacklist that payload. */
    void evaluatePreviousSession() {
        int vc = p.getInt("sess_vc", 0);
        if (vc <= 0) return;
        boolean bad = !p.getBoolean("sess_ready", false) || p.getBoolean("sess_crash", false);
        int fails = bad ? p.getInt("fail_" + vc, 0) + 1 : 0;
        SharedPreferences.Editor e = p.edit().putInt("fail_" + vc, fails).remove("sess_vc");
        e.commit();
        if (fails >= BLACKLIST_AFTER_BAD_SESSIONS) blacklist(vc);
    }

    void startSession(int vc) {
        p.edit().putInt("sess_vc", vc).putBoolean("sess_ready", false).putBoolean("sess_crash", false).putInt("running_vc", vc).commit();
    }
    void clearSession() { p.edit().remove("sess_vc").commit(); }
    void markReady() { p.edit().putBoolean("sess_ready", true).commit(); }
    void markCrash() { p.edit().putBoolean("sess_crash", true).commit(); }
    int runningVc() { return p.getInt("running_vc", 0); }
    void setRunningName(String n) { p.edit().putString("running_name", n).apply(); }
    String runningName() { return p.getString("running_name", ""); }

    /** A verified payload newer than the running one is on disk. */
    boolean updateReady() {
        List<Cand> c = candidates();
        return !c.isEmpty() && c.get(0).vc > runningVc();
    }

    boolean secure() { return p.getBoolean("secure", false); }
    void setSecure(boolean b) { p.edit().putBoolean("secure", b).apply(); }

    // ---- update bookkeeping -----------------------------------------------------------------

    long nextCheck() { return p.getLong("next_check", 0); }
    void setNextCheck(long t) { p.edit().putLong("next_check", t).apply(); }
    String status() { return p.getString("status", "Henüz denetlenmedi"); }
    void setStatus(String s) { p.edit().putString("status", s).putLong("status_ts", System.currentTimeMillis()).apply(); }
    long statusTs() { return p.getLong("status_ts", 0); }
    boolean needsShellUpdate() { return p.getBoolean("needs_shell", false); }
    void setNeedsShellUpdate(boolean b) { p.edit().putBoolean("needs_shell", b).apply(); }

    // ---- APK metadata -----------------------------------------------------------------------

    Meta readMeta(File apk) {
        try {
            PackageInfo pi = ctx.getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(), android.content.pm.PackageManager.GET_META_DATA);
            if (pi == null || pi.applicationInfo == null) return null;
            Meta m = new Meta();
            m.pkg = pi.packageName; m.name = pi.versionName == null ? "" : pi.versionName; m.vc = (int) pi.getLongVersionCode();
            Bundle md = pi.applicationInfo.metaData;
            m.api = md == null ? -1 : md.getInt(Config.API_META, -1);
            return m;
        } catch (Throwable t) { return null; }
    }
}
