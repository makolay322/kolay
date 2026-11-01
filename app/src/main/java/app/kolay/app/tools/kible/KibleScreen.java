package app.kolay.app.tools.kible;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.*;
import android.widget.*;
import app.kolay.app.tools.namaz.NamazTool;
import app.kolay.app.ui.*;
import java.util.Locale;

/**
 * Compass dial with the Kaaba marker. The bearing comes from the saved prayer location (no new permission); the heading
 * from the rotation-vector sensor (fallback: accelerometer + magnetometer), corrected by the local magnetic declination.
 * Sensors run only while this screen is on top and the app is in the foreground.
 */
final class KibleScreen extends Screen implements SensorEventListener {
    private static final double ALIGN_DEG = 3;       // "facing the qibla" tolerance
    private static final float SMOOTH = 0.18f;       // low-pass factor for the heading

    private SensorManager sm;
    private Sensor rotation, accel, magnet;
    private final float[] grav = new float[3], geo = new float[3], rm = new float[9], rr = new float[9], ori = new float[3];
    private boolean haveGrav, haveGeo, listening, aligned;
    private double sx, sy;                            // smoothed heading as a unit vector (avoids the 359°→0° jump)
    private double qibla, declination;
    private int accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;

    private FrameLayout body;
    private Dial dial;
    private TextView turnText, turnSub, hint;
    private Header header;

    @Override public View create() {
        LinearLayout page = new LinearLayout(c.a);
        page.setOrientation(LinearLayout.VERTICAL);
        header = new Header(c, "Kıble", "", () -> c.nav.pop());
        page.addView(header.view);
        body = new FrameLayout(c.a);
        page.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        sm = (SensorManager) c.a.getSystemService(Context.SENSOR_SERVICE);
        if (sm != null) {
            rotation = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnet = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
        render();
        return page;
    }

    private boolean hasCompass() { return rotation != null || (accel != null && magnet != null); }

    /** Builds the body for the current state: no location yet, or the compass. */
    private void render() {
        stop();
        body.removeAllViews();
        double[] loc = c.host.prayerLocation();
        if (loc == null) {
            header.subtitle.setText("Konum gerekli");
            body.addView(Ui.state(c, Icon.Kind.PIN, "Konum gerekli",
                "Kıble yönü kayıtlı konuma göre hesaplanır. Namaz Vakitleri'nde konumu bir kez bulman yeterli.",
                "Konumu bul", () -> c.nav.push(new NamazTool().open(c))), new FrameLayout.LayoutParams(-1, -1));
            return;
        }
        qibla = Qibla.bearing(loc[0], loc[1]);
        declination = new GeomagneticField((float) loc[0], (float) loc[1], 0, System.currentTimeMillis()).getDeclination();
        header.subtitle.setText("Namaz Vakitleri'ndeki konuma göre");
        Palette p = c.p;

        ScrollView sv = new ScrollView(c.a);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout col = new LinearLayout(c.a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(c.dp(Ui.GUTTER), 0, c.dp(Ui.GUTTER), c.dp(Ui.GUTTER));

        LinearLayout card = Ui.card(c, p.surface, 16);
        dial = new Dial();
        int side = Math.min(c.a.getResources().getDisplayMetrics().widthPixels - c.dp(2 * Ui.GUTTER + 32), c.dp(340));
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(side, side);
        dl.gravity = Gravity.CENTER_HORIZONTAL;
        card.addView(dial, dl);
        turnText = Ui.text(c, "", 24, p.ink, 700);
        turnText.setGravity(Gravity.CENTER);
        card.addView(turnText, Ui.lp(-1, -2, 0, c.dp(14), 0, 0));
        turnSub = Ui.text(c, "", 13, p.muted, 400);
        turnSub.setGravity(Gravity.CENTER);
        card.addView(turnSub, Ui.lp(-1, -2, 0, c.dp(6), 0, c.dp(4)));
        col.addView(card, Ui.lp(-1, -2));

        hint = Ui.text(c, "", 13, p.warn, 500);
        hint.setBackground(Ui.rounded(c, p.warnSoft, 14));
        hint.setPadding(c.dp(14), c.dp(10), c.dp(14), c.dp(10));
        hint.setLineSpacing(0, 1.2f);
        hint.setVisibility(View.GONE);
        col.addView(hint, Ui.lp(-1, -2, 0, c.dp(Ui.GAP), 0, 0));

        LinearLayout info = Ui.card(c, p.surface, 16);
        info.addView(row("Kıble açısı", fmt("%.1f° (gerçek kuzeyden)", qibla)));
        info.addView(row("Kâbe'ye uzaklık", String.format(new Locale("tr"), "%,d km", Math.round(Qibla.distanceKm(loc[0], loc[1])))), Ui.lp(-1, -2, 0, c.dp(10), 0, 0));
        info.addView(row("Manyetik sapma", fmt("%+.1f° (otomatik düzeltilir)", declination)), Ui.lp(-1, -2, 0, c.dp(10), 0, 0));
        col.addView(info, Ui.lp(-1, -2, 0, c.dp(Ui.GAP), 0, 0));

        TextView note = Ui.text(c, "Telefonu yere paralel tut; metal eşyalardan, mıknatıslı kılıf ve tutuculardan uzak dur.", 12, p.muted, 400);
        note.setLineSpacing(0, 1.25f);
        col.addView(note, Ui.lp(-1, -2, c.dp(4), c.dp(12), c.dp(4), 0));

        sv.addView(col);
        body.addView(sv, new FrameLayout.LayoutParams(-1, -1));
        if (!hasCompass()) {
            turnText.setText(fmt("%.0f°", qibla));
            turnSub.setText("Bu cihazda pusula sensörü yok: kuzeyden saat yönünde bu açıyı ölç");
        } else {
            turnText.setText("…");
            turnSub.setText("Pusula başlatılıyor");
            start();
        }
    }

    private View row(String k, String v) {
        LinearLayout r = new LinearLayout(c.a);
        TextView kt = Ui.text(c, k, 14, c.p.muted, 400);
        r.addView(kt, new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(Ui.text(c, v, 14, c.p.ink, 600));
        return r;
    }

    private static String fmt(String f, double v) { return String.format(Locale.ROOT, f, v).replace('.', ','); }

    // ---- lifecycle --------------------------------------------------------------------------------

    @Override public void onResume() { if (dial != null) start(); }
    @Override public void onPause() { stop(); }
    @Override public void onDestroy() { stop(); }
    @Override public void onReturn() { render(); }   // back from Namaz Vakitleri: a location may exist now

    private void start() {
        if (listening || sm == null || !hasCompass() || dial == null) return;
        listening = true;
        if (rotation != null) sm.registerListener(this, rotation, SensorManager.SENSOR_DELAY_UI);
        else { sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI); sm.registerListener(this, magnet, SensorManager.SENSOR_DELAY_UI); }
        if (rotation != null && magnet != null) sm.registerListener(this, magnet, SensorManager.SENSOR_DELAY_NORMAL);  // accuracy reports only
    }

    private void stop() {
        if (!listening) return;
        listening = false; haveGrav = haveGeo = false; sx = sy = 0;
        sm.unregisterListener(this);
    }

    // ---- sensors ----------------------------------------------------------------------------------

    @Override public void onAccuracyChanged(Sensor s, int acc) {
        if (s.getType() == Sensor.TYPE_MAGNETIC_FIELD || s.getType() == Sensor.TYPE_ROTATION_VECTOR) accuracy = acc;
    }

    @Override public void onSensorChanged(SensorEvent e) {
        int type = e.sensor.getType();
        if (type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rm, e.values);
        } else if (rotation == null) {
            if (type == Sensor.TYPE_ACCELEROMETER) { System.arraycopy(e.values, 0, grav, 0, 3); haveGrav = true; }
            else if (type == Sensor.TYPE_MAGNETIC_FIELD) { System.arraycopy(e.values, 0, geo, 0, 3); haveGeo = true; }
            if (!haveGrav || !haveGeo || !SensorManager.getRotationMatrix(rm, null, grav, geo)) return;
        } else return;   // magnetometer next to the rotation vector: accuracy only

        remapForDisplay();
        SensorManager.getOrientation(rr, ori);
        double heading = Qibla.norm(Math.toDegrees(ori[0]) + declination);
        double tilt = Math.max(Math.abs(Math.toDegrees(ori[1])), Math.abs(Math.toDegrees(ori[2])));
        double hx = Math.cos(Math.toRadians(heading)), hy = Math.sin(Math.toRadians(heading));
        if (sx == 0 && sy == 0) { sx = hx; sy = hy; } else { sx += (hx - sx) * SMOOTH; sy += (hy - sy) * SMOOTH; }
        update(Qibla.norm(Math.toDegrees(Math.atan2(sy, sx))), tilt);
    }

    /** The sensor frame is fixed to the device; keep "up on screen" as the reference when the display is rotated. */
    @SuppressWarnings("deprecation")
    private void remapForDisplay() {
        int x = SensorManager.AXIS_X, y = SensorManager.AXIS_Y;
        switch (c.a.getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_90: x = SensorManager.AXIS_Y; y = SensorManager.AXIS_MINUS_X; break;
            case Surface.ROTATION_180: x = SensorManager.AXIS_MINUS_X; y = SensorManager.AXIS_MINUS_Y; break;
            case Surface.ROTATION_270: x = SensorManager.AXIS_MINUS_Y; y = SensorManager.AXIS_X; break;
        }
        SensorManager.remapCoordinateSystem(rm, x, y, rr);
    }

    private void update(double heading, double tilt) {
        if (dial == null) return;
        double d = Qibla.turn(heading, qibla);
        boolean now = Math.abs(d) <= ALIGN_DEG;
        if (now && !aligned) Ui.haptic(dial, HapticFeedbackConstants.CONFIRM);
        aligned = now;
        dial.set(heading, aligned);
        int deg = (int) Math.round(Math.abs(d));
        turnText.setText(aligned ? "Kıble karşında" : (d > 0 ? "Sağa " : "Sola ") + deg + "° dön");
        turnText.setTextColor(aligned ? c.p.ok : c.p.ink);
        turnSub.setText(String.format(Locale.ROOT, "Telefonun yönü %d° · kıble %d°", Math.round(heading) % 360, Math.round(qibla) % 360));
        String h = null;
        if (tilt > 40) h = "Telefonu yere paralel tut; dik tutunca pusula şaşar.";
        else if (accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW)
            h = "Pusula ayarsız görünüyor: telefonu havada birkaç kez 8 çizerek çevir.";
        if (h != null) hint.setText(h);
        if ((h != null) != (hint.getVisibility() == View.VISIBLE)) hint.setVisibility(h != null ? View.VISIBLE : View.GONE);
    }

    // ---- dial -------------------------------------------------------------------------------------

    /** Compass rose turning with the phone (north stays north) and a Kaaba marker on the qibla bearing. */
    private final class Dial extends View {
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG), fill = new Paint(Paint.ANTI_ALIAS_FLAG), text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path tri = new Path();
        private double heading;
        private boolean on;

        Dial() {
            super(c.a);
            stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeCap(Paint.Cap.ROUND);
            text.setTextAlign(Paint.Align.CENTER); text.setTypeface(Typeface.create(Typeface.DEFAULT, 700, false));
            setContentDescription("Kıble pusulası");
        }

        void set(double h, boolean aligned) { heading = h; on = aligned; invalidate(); }

        @Override protected void onDraw(Canvas cv) {
            Palette p = c.p;
            float w = getWidth(), cx = w / 2f, cy = getHeight() / 2f;
            float r = Math.min(w, getHeight()) / 2f - c.dp(18);
            int accent = on ? p.ok : p.primary;

            // fixed "phone points here" marker above the ring
            tri.reset();
            tri.moveTo(cx, cy - r - c.dp(14)); tri.lineTo(cx - c.dp(8), cy - r - c.dp(2)); tri.lineTo(cx + c.dp(8), cy - r - c.dp(2)); tri.close();
            fill.setColor(accent); cv.drawPath(tri, fill);

            fill.setColor(on ? p.okSoft : p.surface2);
            cv.drawCircle(cx, cy, r, fill);

            cv.save();
            cv.rotate((float) -heading, cx, cy);
            for (int a = 0; a < 360; a += 5) {
                boolean major = a % 30 == 0;
                stroke.setColor(major ? p.muted : p.line);
                stroke.setStrokeWidth(c.dp(major ? 2 : 1.2f));
                float in = r - c.dp(major ? 12 : 7);
                double t = Math.toRadians(a);
                cv.drawLine(cx + (float) Math.sin(t) * in, cy - (float) Math.cos(t) * in,
                    cx + (float) Math.sin(t) * (r - c.dp(2)), cy - (float) Math.cos(t) * (r - c.dp(2)), stroke);
            }
            text.setTextSize(c.dp(16));
            String[] names = {"K", "D", "G", "B"};
            for (int i = 0; i < 4; i++) {
                text.setColor(i == 0 ? p.danger : p.ink);
                double t = Math.toRadians(i * 90);
                float rr = r - c.dp(28);
                cv.drawText(names[i], cx + (float) Math.sin(t) * rr, cy - (float) Math.cos(t) * rr - (text.descent() + text.ascent()) / 2, text);
            }

            // qibla needle + Kaaba
            cv.rotate((float) qibla, cx, cy);
            stroke.setColor(accent); stroke.setStrokeWidth(c.dp(4));
            float k = c.dp(13), ky = cy - r + c.dp(54);   // inside the letter ring, so it never covers K/D/G/B
            cv.drawLine(cx, cy, cx, ky + k, stroke);
            fill.setColor(p.dark ? 0xFF05080A : 0xFF14201F);
            cv.drawRoundRect(cx - k, ky - k, cx + k, ky + k, c.dp(3), c.dp(3), fill);
            fill.setColor(0xFFD4A537);   // the kiswa's gold band
            cv.drawRect(cx - k, ky - k * 0.45f, cx + k, ky - k * 0.15f, fill);
            cv.restore();

            fill.setColor(accent);
            cv.drawCircle(cx, cy, c.dp(7), fill);
        }
    }
}
