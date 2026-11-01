package app.kolay.payload.tools.apps;

import android.net.Uri;
import android.os.Build;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The three APK sites. Each one finds the newest version of an installed app (by package name) and then opens the
 * download in {@link Browser}. The pages are read with small DOM scripts in the site's own page; the selectors are
 * written defensively (several fallbacks each) because these sites change their markup from time to time. When the
 * automatic path fails, {@link Browser} shows the page so the user can tap the link themselves.
 */
final class Sources {
    private Sources() {}

    static final String[] NAMES = {"APKPure", "APKMirror", "LiteAPKs"};
    static final int APKPURE = 0, APKMIRROR = 1, LITEAPKS = 2;

    /** The installed app being looked up. */
    static final class App {
        final String pkg, label, version; final long code;
        App(String pkg, String label, String version, long code) { this.pkg = pkg; this.label = label; this.version = version; this.code = code; }
    }

    /** A version found on a site and where its download starts. */
    static final class Found {
        final String version, start; final String[] hosts; final String hop; final int hops;
        Found(String version, String start, String[] hosts, String hop, int hops) {
            this.version = version; this.start = start; this.hosts = hosts; this.hop = hop; this.hops = hops;
        }
    }

    interface Result { void found(Found f); void fail(String why); }

    static void find(int source, Browser b, App a, Result r) {
        switch (source) {
            case APKMIRROR: apkMirror(b, a, r); break;
            case LITEAPKS: liteApks(b, a, r); break;
            default: apkPure(b, a, r);
        }
    }

    // ---- APKPure --------------------------------------------------------------------------------

    private static final String[] PURE_HOSTS = {"apkpure.com", "apkpure.net", "pureapk.com", "winudf.com"};

    /** Search page → the app's own page (…/<slug>/<package>). */
    private static final String PURE_SEARCH = """
        (function(){var pkg=%PKG%;var as=document.querySelectorAll('a[href]');
        for(var i=0;i<as.length;i++){var h=as[i].href.split('#')[0].split('?')[0];var k=h.indexOf('/'+pkg);
          if(k<0||h.indexOf('apkpure.')<0)continue;var end=h.charAt(k+pkg.length+1);if(end!==''&&end!=='/')continue;
          return h.substring(0,k+pkg.length+1);}
        return '';})()""";

    /** Download page → version and the d.apkpure.com file link. */
    private static final String PURE_PAGE = """
        (function(){var pkg=%PKG%;var v='';
        var e=document.querySelector('[data-dt-app="'+pkg+'"][data-dt-version]')||document.querySelector('[data-dt-version]');
        if(e)v=e.getAttribute('data-dt-version')||'';
        if(!v){var ld=document.querySelectorAll('script[type="application/ld+json"]');
          for(var i=0;i<ld.length&&!v;i++){try{var arr=[].concat(JSON.parse(ld[i].textContent));
            for(var k=0;k<arr.length;k++)if(arr[k]&&arr[k].softwareVersion){v=String(arr[k].softwareVersion);break;}}catch(x){}}}
        if(!v){var q=document.querySelector('[itemprop=softwareVersion],.version-name,.info-sdk span,.details-sdk span,.ver-name');
          if(q)v=q.getAttribute('content')||q.textContent||'';}
        if(!v){var h=document.querySelector('h1');if(h)v=h.textContent;}
        var dl='';var as=document.querySelectorAll('a[href*="d.apkpure."]');
        for(var j=0;j<as.length;j++){var u=as[j].href;if(u.indexOf(pkg)>=0){dl=u;if(u.indexOf('/APK/')>=0)break;}}
        return JSON.stringify({v:v,dl:dl});})()""";

    private static void apkPure(Browser b, App a, Result r) {
        b.eval("https://apkpure.com/search?q=" + Uri.encode(a.pkg), PURE_HOSTS, js(PURE_SEARCH, a), new Browser.Done() {
            @Override public void ok(String page) {
                if (page == null || page.isEmpty()) { r.fail("APKPure'da bulunamadı"); return; }
                b.eval(page + "/download", PURE_HOSTS, js(PURE_PAGE, a), new Browser.Done() {
                    @Override public void ok(String v) {
                        try {
                            JSONObject o = new JSONObject(v == null ? "{}" : v);
                            String ver = Ver.clean(o.optString("v"));
                            if (ver.isEmpty()) { r.fail("APKPure sayfasında sürüm okunamadı"); return; }
                            String dl = o.optString("dl");
                            if (dl.isEmpty()) dl = "https://d.apkpure.com/b/APK/" + a.pkg + "?version=latest";
                            r.found(new Found(ver, dl, PURE_HOSTS, null, 0));
                        } catch (Exception e) { r.fail("APKPure sayfası okunamadı"); }
                    }
                    @Override public void fail(String why) { r.fail(why); }
                });
            }
            @Override public void fail(String why) { r.fail(why); }
        });
    }

    // ---- APKMirror ------------------------------------------------------------------------------

    private static final String[] MIRROR_HOSTS = {"apkmirror.com"};

    /** Search results (searching by package name): release titles and links, newest first. */
    private static final String MIRROR_SEARCH = """
        (function(){var r=[];document.querySelectorAll('.appRow .appRowTitle a, h5.appRowTitle a').forEach(function(a){
          if(r.length<20)r.push({t:a.textContent.trim(),h:a.href});});
        return r.length?JSON.stringify(r):'';})()""";

    /** Release page: does it belong to this package, and its variants (badge, architecture, Android, dpi). */
    private static final String MIRROR_RELEASE = """
        (function(){var pkg=%PKG%;%HAS%var rows=[];
        document.querySelectorAll('.variants-table .table-row').forEach(function(r){
          var a=r.querySelector('a.accent_color')||r.querySelector('a[href*="-download/"]');if(!a)return;
          var cells=[].map.call(r.querySelectorAll('.table-cell'),function(x){return x.innerText.replace(/\\s+/g,' ').trim();});
          var bd=r.querySelector('.apkm-badge');rows.push({h:a.href,c:cells,b:bd?bd.textContent.trim():''});});
        return JSON.stringify({ok:has(document.body.innerText,pkg),rows:rows});})()""";

    /** Variant page: package check and the "Download APK" button. */
    private static final String MIRROR_VARIANT = """
        (function(){var pkg=%PKG%;%HAS%var a=document.querySelector('a.downloadButton')||document.querySelector('a[href*="/download/?key="]');
        return JSON.stringify({ok:has(document.body.innerText,pkg),dl:a?a.href:''});})()""";

    /** Download page: the file link (it usually also starts by itself). */
    private static final String MIRROR_HOP = """
        (function(){var a=document.querySelector('#download-link')||document.querySelector('a[href*="download.php"]')
          ||document.querySelector('a[rel="nofollow"][href*="/download/"]');return a?a.href:'';})()""";

    private static void apkMirror(Browser b, App a, Result r) {
        String url = "https://www.apkmirror.com/?post_type=app_release&searchtype=apk&s=" + Uri.encode(a.pkg);
        b.eval(url, MIRROR_HOSTS, MIRROR_SEARCH, new Browser.Done() {
            @Override public void ok(String v) {
                List<String[]> releases = new ArrayList<>();
                try {
                    // Group by app (/apk/<developer>/<app>/) and keep each app's newest release that is not a test build.
                    boolean wantsBeta = Ver.isPre(a.version);
                    Map<String, String[]> byApp = new LinkedHashMap<>();
                    JSONArray arr = new JSONArray(v == null ? "[]" : v);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.getJSONObject(i);
                        String t = o.optString("t"), h = o.optString("h");
                        String[] seg = Uri.parse(h).getPath() == null ? new String[0] : Uri.parse(h).getPath().split("/");
                        if (seg.length < 5) continue;
                        String app = seg[2] + "/" + seg[3];
                        if (byApp.containsKey(app) || (!wantsBeta && Ver.isPre(t))) continue;
                        byApp.put(app, new String[]{t, h});
                    }
                    releases.addAll(byApp.values());
                } catch (Exception ignored) { }
                if (releases.isEmpty()) { r.fail("APKMirror'da bulunamadı"); return; }
                tryRelease(b, a, releases, 0, r);
            }
            @Override public void fail(String why) { r.fail(why); }
        });
    }

    /** Checks candidate apps one by one (at most three) until one is this package. */
    private static void tryRelease(Browser b, App a, List<String[]> rel, int i, Result r) {
        if (i >= rel.size() || i >= 3) { r.fail("APKMirror'da bu paket bulunamadı"); return; }
        String title = rel.get(i)[0];
        b.eval(rel.get(i)[1], MIRROR_HOSTS, js(MIRROR_RELEASE, a), new Browser.Done() {
            @Override public void ok(String v) {
                try {
                    JSONObject o = new JSONObject(v == null ? "{}" : v);
                    String variant = pickVariant(o.optJSONArray("rows"));
                    if (variant == null) { tryRelease(b, a, rel, i + 1, r); return; }
                    boolean pkgOnPage = o.optBoolean("ok");
                    b.eval(variant, MIRROR_HOSTS, js(MIRROR_VARIANT, a), new Browser.Done() {
                        @Override public void ok(String v2) {
                            try {
                                JSONObject p = new JSONObject(v2 == null ? "{}" : v2);
                                if (!pkgOnPage && !p.optBoolean("ok")) { tryRelease(b, a, rel, i + 1, r); return; }
                                String dl = p.optString("dl");
                                if (dl.isEmpty()) { r.fail("APKMirror indirme düğmesi bulunamadı"); return; }
                                r.found(new Found(Ver.fromTitle(title), dl, MIRROR_HOSTS, MIRROR_HOP, 2));
                            } catch (Exception e) { r.fail("APKMirror sayfası okunamadı"); }
                        }
                        @Override public void fail(String why) { r.fail(why); }
                    });
                } catch (Exception e) { r.fail("APKMirror sayfası okunamadı"); }
            }
            @Override public void fail(String why) { r.fail(why); }
        });
    }

    /** Prefers a plain APK over a bundle, this phone's architecture (or universal), and nodpi. */
    private static String pickVariant(JSONArray rows) {
        if (rows == null) return null;
        String best = null; int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject o = rows.optJSONObject(i);
            if (o == null) continue;
            JSONArray c = o.optJSONArray("c");
            String arch = c != null && c.length() > 1 ? c.optString(1).toLowerCase(Locale.ROOT) : "";
            String dpi = c != null && c.length() > 3 ? c.optString(3).toLowerCase(Locale.ROOT) : "";
            int s = 0;
            if (o.optString("b").toUpperCase(Locale.ROOT).contains("BUNDLE")) s -= 4;
            if (arch.contains("universal") || arch.contains("noarch")) s += 2;
            else if (arch.contains(Build.SUPPORTED_ABIS[0])) s += 3;
            else {
                boolean any = false;
                for (String abi : Build.SUPPORTED_ABIS) if (arch.contains(abi)) any = true;
                if (!any && !arch.isEmpty()) continue;   // a build for another processor
            }
            if (dpi.contains("nodpi")) s += 1;
            if (s > bestScore) { bestScore = s; best = o.optString("h"); }
        }
        return best == null || best.isEmpty() ? null : best;
    }

    // ---- LiteAPKs -------------------------------------------------------------------------------

    private static final String[] LITE_HOSTS = {"liteapks.com"};

    /** Search results: article links (searching by app name; the package is checked on the app page). */
    private static final String LITE_SEARCH = """
        (function(){var seen={},r=[];document.querySelectorAll('article a[href],.post a[href],h2 a[href],h3 a[href]').forEach(function(a){
          var h=a.href.split('#')[0];if(h.indexOf('liteapks.com/')<0||h.indexOf('/download/')>=0||!/\\.html$/.test(h)||seen[h])return;
          seen[h]=1;if(r.length<6)r.push(h);});
        return r.length?JSON.stringify(r):'';})()""";

    /** App page: package check (Google Play link or text), version row, download button. */
    private static final String LITE_PAGE = """
        (function(){var pkg=%PKG%;%HAS%var ok=has(document.body.innerText,pkg);
        document.querySelectorAll('a[href*="play.google.com"]').forEach(function(a){if(has(a.href,'id='+pkg))ok=true;});
        var v='';var els=document.querySelectorAll('th,td,dt,dd,span,div,li,strong,b,p');
        for(var i=0;i<els.length&&!v;i++){var t=(els[i].childElementCount?'':els[i].textContent).trim().replace(/:$/,'');
          if(/^(version|sürüm)$/i.test(t)){var n=els[i].nextElementSibling;
            v=n?n.textContent:els[i].parentElement.textContent.replace(els[i].textContent,'');}}
        if(!v){var h=document.querySelector('h1');if(h)v=h.textContent;}
        var dl='';document.querySelectorAll('a[href*="/download/"]').forEach(function(a){if(!dl&&a.href.indexOf('liteapks.com/')>=0)dl=a.href;});
        return JSON.stringify({ok:ok,v:v,dl:dl});})()""";

    /** Download pages: a direct file link if there is one, otherwise the next /download/… step. */
    private static final String LITE_HOP = """
        (function(){var as=[].map.call(document.querySelectorAll('a[href]'),function(a){return a.href;})
          .filter(function(h){return h.indexOf('https://')===0;});
        var f=as.filter(function(h){return /\\.(apk|xapk|apks)(\\?|$)/i.test(h);});if(f.length)return f[0];
        var cur=location.href.split('#')[0].replace(/\\/$/,'');
        var d=as.filter(function(h){var x=h.split('#')[0].replace(/\\/$/,'');return x!==cur&&x.indexOf(cur)===0;});
        return d.length?d[0]:'';})()""";

    private static void liteApks(Browser b, App a, Result r) {
        b.eval("https://liteapks.com/?s=" + Uri.encode(a.label), LITE_HOSTS, LITE_SEARCH, new Browser.Done() {
            @Override public void ok(String v) {
                List<String> pages = new ArrayList<>();
                try { JSONArray arr = new JSONArray(v == null ? "[]" : v); for (int i = 0; i < arr.length(); i++) pages.add(arr.getString(i)); }
                catch (Exception ignored) { }
                if (pages.isEmpty()) { r.fail("LiteAPKs'te bulunamadı"); return; }
                tryLite(b, a, pages, 0, r);
            }
            @Override public void fail(String why) { r.fail(why); }
        });
    }

    private static void tryLite(Browser b, App a, List<String> pages, int i, Result r) {
        if (i >= pages.size() || i >= 4) { r.fail("LiteAPKs'te bu paket bulunamadı"); return; }
        b.eval(pages.get(i), LITE_HOSTS, js(LITE_PAGE, a), new Browser.Done() {
            @Override public void ok(String v) {
                try {
                    JSONObject o = new JSONObject(v == null ? "{}" : v);
                    if (!o.optBoolean("ok")) { tryLite(b, a, pages, i + 1, r); return; }
                    String ver = Ver.clean(o.optString("v")), dl = o.optString("dl");
                    if (ver.isEmpty()) { r.fail("LiteAPKs sayfasında sürüm okunamadı"); return; }
                    if (dl.isEmpty()) { r.fail("LiteAPKs indirme düğmesi bulunamadı"); return; }
                    r.found(new Found(ver, dl, LITE_HOSTS, LITE_HOP, 3));
                } catch (Exception e) { r.fail("LiteAPKs sayfası okunamadı"); }
            }
            @Override public void fail(String why) { r.fail(why); }
        });
    }

    // ---- helpers --------------------------------------------------------------------------------

    /**
     * has(text, pkg): the package name occurs as a whole name, so "org.app" does not match "org.app.beta"
     * (package names are letters, digits, '_' and '.').
     */
    private static final String HAS = "function has(t,p){var i=-1;while((i=t.indexOf(p,i+1))>=0){var a=t.charAt(i-1),b=t.charAt(i+p.length),"
        + "c=t.charAt(i+p.length+1);if(!/[A-Za-z0-9_.]/.test(a)&&!(/[A-Za-z0-9_]/.test(b)||(b==='.'&&/[A-Za-z0-9_]/.test(c))))return true;}return false;}";

    /** Puts the package name into a script as a JS string literal (and the {@link #HAS} helper where asked for). */
    private static String js(String script, App a) { return script.replace("%HAS%", HAS).replace("%PKG%", JSONObject.quote(a.pkg)); }

    /** Version strings: cleaning and comparing. */
    static final class Ver {
        private Ver() {}
        private static final Pattern NUM = Pattern.compile("\\d+(?:\\.\\d+)+(?:[-_.]?[A-Za-z]*\\d*)?|\\d+");

        /** "V 11.2.0 (Premium)" → "11.2.0"; prefers a dotted number. */
        static String clean(String s) {
            if (s == null) return "";
            Matcher m = Pattern.compile("\\d+(?:\\.\\d+)+").matcher(s);
            if (m.find()) return m.group();
            m = NUM.matcher(s);
            return m.find() ? m.group() : "";
        }

        /** "Telegram 13.1.1" → "13.1.1": the last dotted number in a release title. */
        static String fromTitle(String t) {
            Matcher m = Pattern.compile("\\d+(?:\\.\\d+)+").matcher(t == null ? "" : t);
            String last = "";
            while (m.find()) last = m.group();
            return last.isEmpty() ? clean(t) : last;
        }

        static boolean isPre(String s) { return s != null && s.toLowerCase(Locale.ROOT).matches(".*\\b(beta|alpha|canary|dev|nightly)\\b.*"); }

        /** Numeric comparison part by part: "1.10" > "1.9"; a missing part counts as 0. */
        static int compare(String a, String b) {
            String[] x = clean(a).split("\\."), y = clean(b).split("\\.");
            for (int i = 0; i < Math.max(x.length, y.length); i++) {
                long p = i < x.length ? num(x[i]) : 0, q = i < y.length ? num(y[i]) : 0;
                if (p != q) return p < q ? -1 : 1;
            }
            return 0;
        }

        private static long num(String s) {
            Matcher m = Pattern.compile("\\d+").matcher(s);
            try { return m.find() ? Long.parseLong(m.group().length() > 15 ? m.group().substring(0, 15) : m.group()) : 0; }
            catch (NumberFormatException e) { return 0; }
        }
    }
}
