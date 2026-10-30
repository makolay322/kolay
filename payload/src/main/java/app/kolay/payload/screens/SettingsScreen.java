package app.kolay.payload.screens;

import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import app.kolay.payload.ui.*;
import java.util.ArrayList;
import java.util.List;

/** API key, privacy switch, update status and about. */
public final class SettingsScreen extends Screen {
    public static final String LLM_SECRET = "llm";

    private final List<View> entrance = new ArrayList<>();
    private TextView keyState, updateState, checkBtn;
    private EditText key;
    private LinearLayout updateBox;

    @Override public View create() {
        Palette p = c.p;
        LinearLayout page = new LinearLayout(c.a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.addView(new Header(c, "Ayarlar", "Kolay", () -> c.nav.pop(), null).view);

        LinearLayout col = new LinearLayout(c.a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(c.dp(16), c.dp(6), c.dp(16), c.dp(32));

        // ---- API key ----
        col.addView(section("GEMINI API ANAHTARI"));
        LinearLayout k = card();
        keyState = Ui.text(c, keyLabel(), 13, p.muted, 500);
        k.addView(row(Icon.Kind.KEY, "Gemini API anahtarı", null, keyState));
        key = new EditText(c.a);
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setSingleLine(true); key.setHint("Anahtarı buraya yapıştır");
        key.setTextColor(p.ink); key.setHintTextColor(p.muted); key.setTextSize(15);
        key.setBackground(Ui.rounded(c, p.surface2, 14));
        key.setPadding(c.dp(14), c.dp(13), c.dp(14), c.dp(13));
        key.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        k.addView(key, Ui.lp(-1, -2, 0, c.dp(12), 0, 0));
        TextView note = Ui.text(c, "Yalnızca bu cihazda, şifreli saklanır. Depoda veya uygulama dosyasında yer almaz.", 12, p.muted, 400);
        note.setLineSpacing(0, 1.15f);
        k.addView(note, Ui.lp(-1, -2, 0, c.dp(10), 0, 0));
        LinearLayout btns = new LinearLayout(c.a);
        TextView save = Ui.button(c, "Kaydet", Ui.Style.PRIMARY, Icon.Kind.CHECK);
        TextView clear = Ui.button(c, "Sil", Ui.Style.TONAL, null);
        save.setOnClickListener(v -> saveKey());
        clear.setOnClickListener(v -> {
            c.host.setSecret(LLM_SECRET, "");
            key.setText(""); Ui.swapText(keyState, keyLabel());
            Ui.haptic(v, HapticFeedbackConstants.CONFIRM);
        });
        btns.addView(save, Ui.lp(0, c.dp(48), 0, 0, c.dp(8), 0));
        ((LinearLayout.LayoutParams) save.getLayoutParams()).weight = 1;
        btns.addView(clear, Ui.lp(c.dp(96), c.dp(48)));
        k.addView(btns, Ui.lp(-1, -2, 0, c.dp(14), 0, 0));
        col.addView(k);

        // ---- privacy ----
        col.addView(section("GİZLİLİK"));
        LinearLayout g = card();
        Toggle t = new Toggle(c.a, p.dark ? p.line : 0xFFC9D2D6, p.primary, p.dark ? p.ink : 0xFFFFFFFF, c.host.secureWindow());
        t.onChange(on -> c.host.setSecureWindow(on));
        g.addView(row(Icon.Kind.SHIELD, "Ekran görüntüsünü engelle", "Son uygulamalar önizlemesini de gizler", t));
        col.addView(g);

        // ---- updates ----
        col.addView(section("GÜNCELLEME"));
        updateBox = card();
        updateState = Ui.text(c, c.host.updateStatus(), 13, p.muted, 400);
        updateState.setLineSpacing(0, 1.15f);
        LinearLayout ur = row(Icon.Kind.REFRESH, "Otomatik güncelleme", null, null);
        updateBox.addView(ur);
        updateBox.addView(updateState, Ui.lp(-1, -2, 0, c.dp(10), 0, 0));
        checkBtn = Ui.button(c, "Şimdi denetle", Ui.Style.TONAL, null);
        checkBtn.setOnClickListener(v -> checkNow());
        updateBox.addView(checkBtn, Ui.lp(-1, c.dp(48), 0, c.dp(14), 0, 0));
        renderUpdateButtons();
        col.addView(updateBox);

        // ---- about ----
        col.addView(section("HAKKINDA"));
        LinearLayout ab = card();
        ab.addView(row(Icon.Kind.SCAN, "Modül", c.host.payloadName() + " (v" + c.host.payloadVersion() + ")", null));
        ab.addView(divider());
        ab.addView(row(Icon.Kind.SHIELD, "Kabuk", "sürüm " + c.host.shellVersion(), null));
        ab.addView(divider());
        LinearLayout gh = row(Icon.Kind.LINK, "Sürüm sayfası", "GitHub", Ui.icon(c, Icon.Kind.CHEVRON, p.muted, 18));
        gh.setClickable(true);
        Ui.surface(c, gh, p.surface, 12, false);
        gh.setOnClickListener(v -> c.a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(c.host.releasesUrl()))));
        ab.addView(gh);
        col.addView(ab);

        for (int i = 0; i < col.getChildCount(); i++) entrance.add(col.getChildAt(i));
        for (View v : entrance) v.setAlpha(0f);

        ScrollView sv = new ScrollView(c.a);
        sv.setVerticalScrollBarEnabled(false);
        sv.setClipToPadding(false);
        sv.addView(col);
        page.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        return page;
    }

    @Override public void onShown() {
        long d = 0;
        for (View v : entrance) { Ui.appear(v, d); d += 35; }
    }

    private String keyLabel() { return c.host.secret(LLM_SECRET).isEmpty() ? "Ayarlanmadı" : "Kayıtlı"; }

    private void saveKey() {
        String s = key.getText().toString().trim();
        if (s.isEmpty()) { Toast.makeText(c.a, "Önce anahtarı yapıştır", Toast.LENGTH_SHORT).show(); return; }
        try {
            c.host.setSecret(LLM_SECRET, s);
            key.setText("");
            Ui.swapText(keyState, keyLabel());
            Ui.haptic(key, HapticFeedbackConstants.CONFIRM);
            Toast.makeText(c.a, "Kaydedildi", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { Toast.makeText(c.a, e.getMessage(), Toast.LENGTH_LONG).show(); }
    }

    private void checkNow() {
        checkBtn.setEnabled(false);
        Ui.swapText(checkBtn, "Denetleniyor…");
        c.host.checkForUpdates(status -> {
            checkBtn.setEnabled(true);
            Ui.swapText(checkBtn, "Şimdi denetle");
            Ui.swapText(updateState, status);
            renderUpdateButtons();
        });
    }

    private void renderUpdateButtons() {
        if (c.host.updateReady()) {
            checkBtn.setText("Şimdi yenile");
            Ui.style(c, checkBtn, Ui.Style.PRIMARY, Icon.Kind.REFRESH);
            checkBtn.setOnClickListener(v -> { Ui.haptic(v, HapticFeedbackConstants.CONFIRM); c.host.restart(); });
        }
    }

    // ---- small building blocks -------------------------------------------------------------

    private TextView section(String s) {
        TextView t = Ui.text(c, s, 12, c.p.muted, 600);
        t.setLetterSpacing(0.1f);
        t.setLayoutParams(Ui.lp(-2, -2, c.dp(6), c.dp(24), 0, c.dp(10)));
        return t;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(c.a);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(c.dp(16), c.dp(16), c.dp(16), c.dp(16));
        Ui.surface(c, l, c.p.surface, 22, true);
        l.setLayoutParams(Ui.lp(-1, -2));
        return l;
    }

    private View divider() {
        View v = new View(c.a);
        v.setBackgroundColor(c.p.line);
        v.setLayoutParams(Ui.lp(-1, 1, 0, c.dp(12), 0, c.dp(12)));
        return v;
    }

    private LinearLayout row(Icon.Kind icon, String title, String sub, View trailing) {
        LinearLayout r = new LinearLayout(c.a);
        r.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout tile = new FrameLayout(c.a);
        tile.setBackground(Ui.rounded(c, c.p.primarySoft, 12));
        tile.addView(Ui.icon(c, icon, c.p.onPrimarySoft, 20), new FrameLayout.LayoutParams(c.dp(20), c.dp(20), Gravity.CENTER));
        r.addView(tile, new LinearLayout.LayoutParams(c.dp(40), c.dp(40)));
        LinearLayout t = new LinearLayout(c.a);
        t.setOrientation(LinearLayout.VERTICAL);
        t.addView(Ui.text(c, title, 16, c.p.ink, 600));
        if (sub != null) t.addView(Ui.text(c, sub, 13, c.p.muted, 400), Ui.lp(-2, -2, 0, c.dp(2), 0, 0));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.setMarginStart(c.dp(14)); lp.setMarginEnd(c.dp(8));
        r.addView(t, lp);
        if (trailing != null) r.addView(trailing);
        return r;
    }
}
