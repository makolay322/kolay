package app.kolay.payload.tools.namaz;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import app.kolay.payload.ui.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Today's prayer times for the saved location, with a live countdown, the reminder switch and the widget button.
 * The calculation, alarms and widget live in the shell (they must work while the app is closed); this is only the front end.
 */
final class NamazScreen extends Screen {
    private static final int SHELL_NEEDED = 7, REQ_LOC = 71, REQ_NOTIF = 72;
    private static final String[] NAMES = {"Sabah", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı"};

    private Header header;
    private LinearLayout body;
    private TextView heroName, heroTime, heroLeft;
    private Toggle toggle;
    private long[] today, tomorrow;
    private long nextAt;                // the instant the countdown runs to
    private boolean destroyed, ticking;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (destroyed || !ticking) return;
            countdown();
            c.ui.postDelayed(this, 1000);
        }
    };

    @Override public View create() {
        LinearLayout page = new LinearLayout(c.a);
        page.setOrientation(LinearLayout.VERTICAL);
        header = new Header(c, "Namaz Vakitleri", "Konuma göre", () -> c.nav.pop());
        page.addView(header.view);
        ScrollView sv = new ScrollView(c.a);
        sv.setFillViewport(true);
        body = new LinearLayout(c.a);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(c.dp(20), c.dp(4), c.dp(20), c.dp(24));
        sv.addView(body, new FrameLayout.LayoutParams(-1, -2));
        page.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        return page;
    }

    @Override public void onShown() { start(); }
    @Override public void onResume() { if (body != null && body.getChildCount() > 0) { ticking = true; c.ui.removeCallbacks(tick); c.ui.post(tick); } }
    @Override public void onPause() { ticking = false; c.ui.removeCallbacks(tick); }
    @Override public void onDestroy() { destroyed = true; ticking = false; c.ui.removeCallbacks(tick); }

    // ---- flow ---------------------------------------------------------------------------------

    private void start() {
        if (c.host.shellVersion() < SHELL_NEEDED) {
            info("Bu özellik için kabuk güncellemesi gerekli", "Ayarlar → “Kabuğu indir ve yükle”. Widget ve hatırlatıcı kabukta çalışır.");
            return;
        }
        if (hasLocationPermission()) locate(c.host.prayerLocation() != null);
        if (c.host.prayerLocation() != null) render();
        else if (!hasLocationPermission()) askLocation();
    }

    private boolean hasLocationPermission() { return c.a.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED; }

    private void askLocation() {
        body.removeAllViews();
        body.addView(Ui.text(c, "Konum gerekli", 20, c.p.ink, 700), Ui.lp(-2, -2, 0, c.dp(16), 0, 0));
        TextView t = Ui.text(c, "Vakitler bulunduğun yere göre hesaplanır. Konum yalnızca cihazında kalır; internete gönderilmez.", 14, c.p.muted, 400);
        t.setLineSpacing(0, 1.25f);
        body.addView(t, Ui.lp(-1, -2, 0, c.dp(8), 0, c.dp(20)));
        TextView go = Ui.button(c, "Konumu bul", Ui.Style.PRIMARY, Icon.Kind.SCAN);
        go.setOnClickListener(v -> c.a.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOC));
        body.addView(go, Ui.lp(-1, c.dp(54)));
    }

    @Override public void onPermission(int req, String[] perms, int[] r) {
        boolean ok = r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED;
        if (req == REQ_LOC) {
            if (ok) locate(false); else info("Konum izni verilmedi", "Ayarlar → Uygulamalar → Kolay → İzinler → Konum.");
        } else if (req == REQ_NOTIF) {
            if (ok) { c.host.prayerSetReminder(true); }
            else { if (toggle != null) toggle.set(false, true); say("Bildirim izni olmadan hatırlatıcı gösterilemez"); }
        }
    }

    /** Takes the most recent known position, or asks the system for a fresh one. {@code quiet}: no error message when it fails. */
    private void locate(boolean quiet) {
        try {
            LocationManager lm = (LocationManager) c.a.getSystemService(Context.LOCATION_SERVICE);
            Location best = null;
            for (String pv : lm.getProviders(true)) {
                try {
                    Location l = lm.getLastKnownLocation(pv);
                    if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
                } catch (SecurityException | IllegalArgumentException ignored) { }   // GPS needs the fine permission
            }
            if (best != null) { apply(best); return; }
            String pv = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ? LocationManager.NETWORK_PROVIDER : null;
            if (pv == null || Build.VERSION.SDK_INT < 30) { if (!quiet) fail(); return; }
            lm.getCurrentLocation(pv, null, c.a.getMainExecutor(), loc -> {
                if (destroyed) return;
                if (loc != null) apply(loc); else if (!quiet) fail();
            });
        } catch (Throwable t) { if (!quiet) fail(); }
    }

    private void fail() {
        info("Konum alınamadı", "Cihazın konum servisini açıp tekrar dene.");
    }

    private void apply(Location l) {
        double[] old = c.host.prayerLocation();
        if (old == null || Math.abs(old[0] - l.getLatitude()) > 0.05 || Math.abs(old[1] - l.getLongitude()) > 0.05)   // ~5 km: no churn from GPS jitter
            c.host.prayerSetLocation(l.getLatitude(), l.getLongitude());
        render();
    }

    // ---- views --------------------------------------------------------------------------------

    private void info(String title, String text) {
        body.removeAllViews();
        TextView t = Ui.text(c, title, 18, c.p.ink, 600); t.setGravity(Gravity.CENTER);
        TextView b = Ui.text(c, text, 14, c.p.muted, 400); b.setGravity(Gravity.CENTER);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        body.addView(t, Ui.lp(-1, -2, 0, c.dp(48), 0, 0));
        body.addView(b, Ui.lp(-1, -2, 0, c.dp(8), 0, 0));
    }

    private void load() {
        Calendar now = Calendar.getInstance();
        today = c.host.prayerTimes(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
        now.add(Calendar.DAY_OF_YEAR, 1);
        tomorrow = c.host.prayerTimes(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
    }

    /** {index, instant, day} of the next prayer start (sunrise skipped); day 0 = today, 1 = tomorrow. */
    private long[] next(long now) {
        for (int d = 0; d < 2; d++) {
            long[] t = d == 0 ? today : tomorrow;
            if (t == null) return null;
            for (int i = 0; i < 6; i++) if (i != 1 && t[i] > now) return new long[]{i, t[i], d};
        }
        return null;
    }

    private void render() {
        if (destroyed) return;
        load();
        if (today == null) { askLocation(); return; }
        double[] loc = c.host.prayerLocation();
        header.subtitle.setText(String.format(Locale.US, "%.2f° K · %.2f° D", loc[0], Math.abs(loc[1])).replace("° D", loc[1] >= 0 ? "° D" : "° B"));

        body.removeAllViews();
        body.setGravity(Gravity.NO_GRAVITY);
        long now = System.currentTimeMillis();
        long[] n = next(now);

        LinearLayout hero = new LinearLayout(c.a);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(c.dp(20), c.dp(18), c.dp(20), c.dp(18));
        hero.setBackground(Ui.rounded(c, c.p.primarySoft, 20));
        hero.addView(Ui.text(c, "Sıradaki vakit", 13, c.p.onPrimarySoft, 500));
        LinearLayout row = new LinearLayout(c.a);
        row.setGravity(Gravity.BOTTOM);
        heroName = Ui.text(c, "", 26, c.p.onPrimarySoft, 700);
        heroTime = Ui.text(c, "", 44, c.p.onPrimarySoft, 700);
        row.addView(heroName, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(heroTime);
        hero.addView(row, Ui.lp(-1, -2, 0, c.dp(6), 0, 0));
        heroLeft = Ui.text(c, "", 15, c.p.onPrimarySoft, 500);
        hero.addView(heroLeft, Ui.lp(-1, -2, 0, c.dp(6), 0, 0));
        body.addView(hero, Ui.lp(-1, -2, 0, c.dp(8), 0, c.dp(16)));
        if (n != null) {
            heroName.setText(NAMES[(int) n[0]]);
            heroTime.setText(clock(n[1]));
            nextAt = n[1];
        }

        for (int i = 0; i < 6; i++) {
            boolean isNext = n != null && n[2] == 0 && n[0] == i;
            LinearLayout r = new LinearLayout(c.a);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(c.dp(16), 0, c.dp(16), 0);
            if (isNext) r.setBackground(Ui.rounded(c, c.p.surface2, 14));
            TextView nm = Ui.text(c, NAMES[i], 16, isNext ? c.p.ink : c.p.muted, isNext ? 700 : 500);
            TextView tm = Ui.text(c, clock(today[i]), 18, c.p.ink, isNext ? 700 : 500);
            r.addView(nm, new LinearLayout.LayoutParams(0, -2, 1));
            r.addView(tm);
            body.addView(r, Ui.lp(-1, c.dp(48)));
        }

        // reminder switch
        LinearLayout rem = new LinearLayout(c.a);
        rem.setGravity(Gravity.CENTER_VERTICAL);
        rem.setPadding(c.dp(16), c.dp(14), c.dp(16), c.dp(14));
        rem.setBackground(Ui.rounded(c, c.p.surface, 16));
        LinearLayout txt = new LinearLayout(c.a);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(Ui.text(c, "Vakit çıkmadan 15 dk önce hatırlat", 15, c.p.ink, 600));
        txt.addView(Ui.text(c, "Her vakit için, vakit bitmeden bildirim gelir", 12, c.p.muted, 400), Ui.lp(-2, -2, 0, c.dp(2), 0, 0));
        toggle = new Toggle(c.a, c.p.dark ? c.p.line : 0xFFC9D2D6, c.p.primary, c.p.dark ? c.p.ink : 0xFFFFFFFF, c.host.prayerReminder());
        toggle.onChange(this::onReminder);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, -2, 1);
        tl.setMarginEnd(c.dp(12));
        rem.addView(txt, tl);
        rem.addView(toggle);
        body.addView(rem, Ui.lp(-1, -2, 0, c.dp(16), 0, 0));

        if (Build.VERSION.SDK_INT >= 31 && !canExact()) {
            TextView exact = Ui.button(c, "Tam zamanlı alarm izni ver", Ui.Style.GHOST, Icon.Kind.CLOCK);
            exact.setOnClickListener(v -> c.a.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + c.a.getPackageName()))));
            body.addView(exact, Ui.lp(-1, c.dp(44), 0, c.dp(4), 0, 0));
        }

        TextView pin = Ui.button(c, "Ana ekrana widget ekle", Ui.Style.TONAL, Icon.Kind.CLOCK);
        pin.setOnClickListener(v -> { if (!c.host.prayerPinWidget()) say("Ana ekrana uzun bas → Widget'lar → Kolay → Namaz vakti"); });
        body.addView(pin, Ui.lp(-1, c.dp(52), 0, c.dp(12), 0, 0));
        TextView loc2 = Ui.button(c, "Konumu güncelle", Ui.Style.GHOST, Icon.Kind.REFRESH);
        loc2.setOnClickListener(v -> { if (hasLocationPermission()) { locate(false); } else c.a.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOC); });
        body.addView(loc2, Ui.lp(-1, c.dp(48), 0, c.dp(4), 0, 0));

        TextView note = Ui.text(c, "Astronomik hesap: Diyanet açıları (imsak 18°, yatsı 17°), Hanefi ikindi ve küçük ihtiyat payları. Resmî takvimle birkaç dakika fark olabilir; yüksek enlemlerde yatsı/imsak yaklaşık hesaplanır.", 12, c.p.muted, 400);
        note.setLineSpacing(0, 1.2f);
        body.addView(note, Ui.lp(-1, -2, 0, c.dp(12), 0, 0));

        ticking = true;
        c.ui.removeCallbacks(tick);
        c.ui.post(tick);
    }

    private boolean canExact() {
        AlarmManager am = c.a.getSystemService(AlarmManager.class);
        return am == null || am.canScheduleExactAlarms();
    }

    private void onReminder(boolean on) {
        if (!on) { c.host.prayerSetReminder(false); return; }
        if (c.host.prayerLocation() == null) { toggle.set(false, true); say("Önce konum gerekli"); return; }
        if (Build.VERSION.SDK_INT >= 33 && c.a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            c.a.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
            return;
        }
        c.host.prayerSetReminder(true);
    }

    // ---- countdown ----------------------------------------------------------------------------

    private void countdown() {
        if (heroLeft == null) return;
        long left = nextAt - System.currentTimeMillis();
        if (left <= 0) { render(); return; }     // the time arrived: pick the following one
        long s = left / 1000;
        heroLeft.setText(String.format(Locale.US, "%d:%02d:%02d kaldı", s / 3600, (s / 60) % 60, s % 60));
    }

    private static String clock(long t) { return new SimpleDateFormat("HH:mm", Locale.GERMANY).format(new Date(t)); }

    private void say(String s) { Toast.makeText(c.a, s, Toast.LENGTH_LONG).show(); }
}
