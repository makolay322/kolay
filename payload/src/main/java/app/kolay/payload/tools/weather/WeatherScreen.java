package app.kolay.payload.tools.weather;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import app.kolay.payload.ui.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/** Place field -> 7-day forecast with rain chance. The last searched place name is remembered on the device. */
final class WeatherScreen extends Screen {
    private static final String PREFS = "weather", KEY_PLACE = "place";

    private EditText place;
    private TextView status;
    private LinearLayout out;
    private volatile boolean destroyed;
    private boolean busy;

    @Override public View create() {
        Palette p = c.p;
        LinearLayout page = new LinearLayout(c.a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.addView(new Header(c, "Hava Durumu", "Şehir veya ilçe ara", () -> c.nav.pop()).view);

        LinearLayout col = new LinearLayout(c.a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(c.dp(16), c.dp(6), c.dp(16), c.dp(24));

        place = new EditText(c.a);
        place.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        place.setSingleLine(true); place.setHint("Örn. Ankara");
        place.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        place.setTextColor(p.ink); place.setHintTextColor(p.muted); place.setTextSize(15);
        place.setBackground(Ui.rounded(c, p.surface2, 14));
        place.setPadding(c.dp(14), c.dp(13), c.dp(14), c.dp(13));
        place.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        place.setOnEditorActionListener((v, id, e) -> { start(); return true; });
        col.addView(place, Ui.lp(-1, -2));

        TextView go = Ui.button(c, "Tahmini getir", Ui.Style.PRIMARY, Icon.Kind.REFRESH);
        go.setOnClickListener(v -> start());
        col.addView(go, Ui.lp(-1, c.dp(52), 0, c.dp(12), 0, 0));

        status = Ui.text(c, "", 13, p.muted, 500);
        status.setVisibility(View.GONE);
        col.addView(status, Ui.lp(-1, -2, c.dp(4), c.dp(16), 0, 0));

        out = new LinearLayout(c.a);
        out.setOrientation(LinearLayout.VERTICAL);
        col.addView(out, Ui.lp(-1, -2, 0, c.dp(12), 0, 0));

        ScrollView sv = new ScrollView(c.a);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(col);
        page.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));

        String last = prefs().getString(KEY_PLACE, "");
        if (!last.isEmpty()) { place.setText(last); start(); }
        return page;
    }

    @Override public void onDestroy() { destroyed = true; }

    private SharedPreferences prefs() { return c.a.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    private void setStatus(String s) {
        status.setText(s);
        status.setVisibility(s.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void start() {
        String q = place.getText().toString().trim();
        if (busy || q.isEmpty()) return;
        busy = true;
        setStatus("Tahmin alınıyor…");
        out.removeAllViews();
        c.io.execute(() -> {
            Forecast f = null; String err = null;
            try { f = Forecast.fetch(q); }
            catch (Exception e) { err = e.getMessage() == null ? "Tahmin alınamadı" : e.getMessage(); }
            final Forecast ff = f; final String ee = err;
            c.ui.post(() -> {
                busy = false;
                if (destroyed) return;
                if (ff == null) { setStatus(ee); return; }
                prefs().edit().putString(KEY_PLACE, q).apply();
                setStatus("");
                show(ff);
            });
        });
    }

    private void show(Forecast f) {
        Palette p = c.p;
        LinearLayout head = new LinearLayout(c.a);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(c.dp(18), c.dp(16), c.dp(18), c.dp(16));
        Ui.surface(c, head, p.primarySoft, 22, false);
        head.addView(Ui.text(c, f.place, 14, p.onPrimarySoft, 600));
        head.addView(Ui.text(c, Math.round(f.nowTemp) + "°", 44, p.onPrimarySoft, 700), Ui.lp(-2, -2, 0, c.dp(4), 0, 0));
        head.addView(Ui.text(c, "Şu an · " + Forecast.describe(f.nowCode), 14, p.onPrimarySoft, 400));
        out.addView(head, Ui.lp(-1, -2, 0, 0, 0, c.dp(12)));
        Ui.appear(head, 0);

        long d = 60;
        for (int i = 0; i < f.days.length; i++) {
            View row = dayRow(f.days[i], i == 0);
            out.addView(row, Ui.lp(-1, -2, 0, 0, 0, c.dp(8)));
            Ui.appear(row, d); d += 45;
        }
    }

    private View dayRow(Forecast.Day x, boolean today) {
        Palette p = c.p;
        LinearLayout row = new LinearLayout(c.a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(c.dp(16), c.dp(12), c.dp(16), c.dp(12));
        Ui.surface(c, row, p.surface, 18, true);

        String name = today ? "Bugün" : LocalDate.parse(x.date).getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("tr"));
        LinearLayout left = new LinearLayout(c.a);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(Ui.text(c, name, 15, p.ink, 600));
        left.addView(Ui.text(c, Forecast.describe(x.code), 13, p.muted, 400), Ui.lp(-2, -2, 0, c.dp(2), 0, 0));
        row.addView(left, new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout right = new LinearLayout(c.a);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END);
        right.addView(Ui.text(c, Math.round(x.max) + "° / " + Math.round(x.min) + "°", 15, p.ink, 600));
        String rain = "Yağış %" + Math.round(x.rainChance) + (x.rainMm >= 0.1 ? " · " + String.format(Locale.US, "%.1f", x.rainMm) + " mm" : "");
        right.addView(Ui.text(c, rain, 13, x.rainChance >= 50 ? p.primary : p.muted, 500), Ui.lp(-2, -2, 0, c.dp(2), 0, 0));
        row.addView(right, new LinearLayout.LayoutParams(-2, -2));
        return row;
    }
}
