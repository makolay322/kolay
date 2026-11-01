package app.kolay.payload.tools.halal;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.PersistableBundle;
import android.view.*;
import android.widget.*;
import app.kolay.payload.ui.*;

/** The assessment: a colored verdict, the reasons, and the ingredient text it was based on. Only in memory. */
final class VerdictScreen extends Screen {
    private final String name, source, ingredients, error;
    private final Halal.Verdict verdict;
    private final View[] parts = new View[4];

    VerdictScreen(String name, String source, String ingredients, Halal.Verdict verdict, String error) {
        this.name = name; this.source = source; this.ingredients = ingredients; this.verdict = verdict; this.error = error;
    }

    @Override public View create() {
        Palette p = c.p;
        LinearLayout page = new LinearLayout(c.a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.addView(new Header(c, "Sonuç", source.isEmpty() ? "Hanefi mezhebi" : source + " · Hanefi mezhebi", this::again).view);

        LinearLayout col = new LinearLayout(c.a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(c.dp(16), c.dp(4), c.dp(16), c.dp(16));

        if (error != null) {
            TextView t = Ui.text(c, "Değerlendirme yapılamadı\n\n" + error, 15, p.danger, 500);
            t.setLineSpacing(0, 1.3f);
            LinearLayout card = card(p.surface);
            card.addView(t);
            col.addView(card, Ui.lp(-1, -2));
            parts[0] = card;
        } else {
            int bg, fg; String label;
            switch (verdict.level) {
                case HELAL: bg = p.okSoft; fg = p.ok; label = "HELAL"; break;
                case HARAM: bg = p.dangerSoft; fg = p.danger; label = "HARAM"; break;
                case SUPHELI: bg = p.warnSoft; fg = p.warn; label = "ŞÜPHELİ"; break;
                default: bg = p.surface2; fg = p.ink; label = "BELİRSİZ"; break;
            }
            LinearLayout badge = new LinearLayout(c.a);
            badge.setOrientation(LinearLayout.VERTICAL); badge.setGravity(Gravity.CENTER);
            badge.setPadding(c.dp(20), c.dp(24), c.dp(20), c.dp(24));
            badge.setBackground(Ui.rounded(c, bg, 22));
            TextView v = Ui.text(c, label, 34, fg, 700);
            v.setLetterSpacing(0.04f);
            badge.addView(v);
            if (!name.isEmpty()) {
                TextView n = Ui.text(c, name, 15, fg, 500);
                n.setGravity(Gravity.CENTER);
                badge.addView(n, Ui.lp(-2, -2, 0, c.dp(8), 0, 0));
            }
            col.addView(badge, Ui.lp(-1, -2));
            parts[0] = badge;

            LinearLayout card = card(p.surface);
            TextView d = Ui.text(c, verdict.detail, 15, p.ink, 400);
            d.setLineSpacing(0, 1.3f);
            card.addView(d);
            col.addView(card, Ui.lp(-1, -2, 0, c.dp(12), 0, 0));
            parts[1] = card;
        }

        if (!ingredients.isEmpty()) {
            TextView lab = Ui.text(c, "DEĞERLENDİRİLEN İÇİNDEKİLER", 12, p.muted, 600);
            lab.setLetterSpacing(0.1f);
            col.addView(lab, Ui.lp(-2, -2, c.dp(4), c.dp(20), 0, c.dp(8)));
            LinearLayout card = card(p.surface2);
            String shown = ingredients.length() > 1200 ? ingredients.substring(0, 1200) + "…" : ingredients;
            TextView t = Ui.text(c, shown, 13, p.muted, 400);
            t.setLineSpacing(0, 1.25f);
            card.addView(t);
            col.addView(card, Ui.lp(-1, -2));
            parts[2] = card;
        }
        TextView note = Ui.text(c, "Otomatik değerlendirmedir, fetva değildir. Şüphede üreticiye veya yetkili bir kuruma danış.", 12, p.muted, 400);
        note.setLineSpacing(0, 1.2f);
        col.addView(note, Ui.lp(-1, -2, c.dp(4), c.dp(16), c.dp(4), 0));

        ScrollView sv = new ScrollView(c.a);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(col);
        page.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout row = new LinearLayout(c.a);
        row.setPadding(c.dp(16), c.dp(8), c.dp(16), c.dp(16));
        TextView copy = Ui.button(c, "Kopyala", Ui.Style.TONAL, Icon.Kind.COPY);
        TextView again = Ui.button(c, "Yeni sorgu", Ui.Style.PRIMARY, Icon.Kind.BARCODE);
        copy.setOnClickListener(x -> copy(copy));
        again.setOnClickListener(x -> again());
        LinearLayout.LayoutParams l1 = Ui.lp(0, c.dp(56), 0, 0, c.dp(10), 0); l1.weight = 1;
        LinearLayout.LayoutParams l2 = Ui.lp(0, c.dp(56)); l2.weight = 1;
        row.addView(copy, l1); row.addView(again, l2);
        page.addView(row);
        if (error != null) copy.setVisibility(View.GONE);
        return page;
    }

    private LinearLayout card(int fill) {
        LinearLayout card = new LinearLayout(c.a);
        card.setPadding(c.dp(18), c.dp(18), c.dp(18), c.dp(18));
        Ui.surface(c, card, fill, 22, fill == c.p.surface);
        return card;
    }

    @Override public void onShown() {
        long d = 0;
        for (View v : parts) if (v != null) { Ui.appear(v, d); d += 70; }
        if (verdict != null) Ui.haptic(parts[0], HapticFeedbackConstants.CONFIRM);
    }

    private void copy(TextView btn) {
        String text = (name.isEmpty() ? "" : name + "\n") + verdict.level + "\n" + verdict.detail;
        ClipData clip = ClipData.newPlainText("Helal", text);
        PersistableBundle extras = new PersistableBundle();
        extras.putBoolean("android.content.extra.IS_SENSITIVE", true);
        clip.getDescription().setExtras(extras);
        ((ClipboardManager) c.a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(clip);
        Ui.haptic(btn, HapticFeedbackConstants.CONFIRM);
        btn.setText("Kopyalandı");
        c.ui.postDelayed(() -> { if (btn.isAttachedToWindow()) btn.setText("Kopyala"); }, 1600);
    }

    private void again() { c.nav.replace(new ScanScreen(null)); }
    @Override public boolean onBack() { again(); return true; }
}
