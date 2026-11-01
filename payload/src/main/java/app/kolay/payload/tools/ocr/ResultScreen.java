package app.kolay.payload.tools.ocr;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.view.*;
import android.widget.*;
import app.kolay.payload.ui.*;

/** The cleaned text with Copy and Share. Never stored: it only exists in memory while this screen is open. */
final class ResultScreen extends Screen {
    private final Session s;
    private final String raw;
    private String text;            // what is shown / copied: the raw OCR text, or its cleaned version in general mode
    private boolean cleaned, cleaning;
    private TextView out, clean;
    private final long secs;
    private final int pages;
    private final boolean ok;
    private View body, bar;
    private TextView copy;

    ResultScreen(Session s, String text, long secs, int pages, boolean ok) { this.s = s; this.raw = text; this.text = text; this.secs = secs; this.pages = pages; this.ok = ok; }

    @Override public View create() {
        Palette p = c.p;
        LinearLayout page = new LinearLayout(c.a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.addView(new Header(c, "Sonuç", secs + " sn · " + pages + " sayfa", this::back).view);

        ScrollView sv = new ScrollView(c.a);
        sv.setVerticalScrollBarEnabled(false);
        TextView t = Ui.text(c, text, 15, ok ? p.ink : p.danger, 400);
        out = t;
        t.setLineSpacing(0, 1.3f);
        t.setTextIsSelectable(false);
        LinearLayout card = new LinearLayout(c.a);
        card.setPadding(c.dp(18), c.dp(18), c.dp(18), c.dp(18));
        Ui.surface(c, card, p.surface, 22, true);
        card.addView(t);
        FrameLayout wrap = new FrameLayout(c.a);
        wrap.setPadding(c.dp(16), c.dp(4), c.dp(16), c.dp(16));
        wrap.addView(card);
        sv.addView(wrap);
        body = sv;
        page.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout bottom = new LinearLayout(c.a);
        bottom.setOrientation(LinearLayout.VERTICAL);
        if (s.general && ok) {   // general mode: the text stays on the device unless this button is pressed
            clean = Ui.button(c, "Gemini'ye gönder (temizle)", Ui.Style.TONAL, Icon.Kind.REFRESH);
            clean.setOnClickListener(v -> cleanUp());
            bottom.addView(clean, Ui.lp(-1, c.dp(52), c.dp(16), c.dp(8), c.dp(16), 0));
        }
        LinearLayout row = new LinearLayout(c.a);
        row.setPadding(c.dp(16), c.dp(8), c.dp(16), c.dp(16));
        copy = Ui.button(c, "Kopyala", Ui.Style.PRIMARY, Icon.Kind.COPY);
        TextView share = Ui.button(c, "Paylaş", Ui.Style.TONAL, Icon.Kind.SHARE);
        copy.setOnClickListener(v -> copy());
        share.setOnClickListener(v -> {
            Ui.haptic(v, HapticFeedbackConstants.VIRTUAL_KEY);
            c.a.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null));
        });
        row.addView(copy, Ui.lp(0, c.dp(56), 0, 0, c.dp(10), 0));
        ((LinearLayout.LayoutParams) copy.getLayoutParams()).weight = 1;
        row.addView(share, Ui.lp(0, c.dp(56)));
        ((LinearLayout.LayoutParams) share.getLayoutParams()).weight = 1;
        bottom.addView(row);
        bar = bottom;
        page.addView(bottom);
        body.setAlpha(0f); bar.setAlpha(0f);
        return page;
    }

    @Override public void onShown() {
        body.setTranslationY(c.dp(20));
        body.animate().alpha(1f).translationY(0).setDuration(420).setInterpolator(Ui.EMPH).start();
        bar.setTranslationY(c.dp(40));
        bar.animate().alpha(1f).translationY(0).setStartDelay(120).setDuration(420).setInterpolator(Ui.EMPH).start();
    }

    /** General mode only: sends the OCR text to the model with a loose cleanup prompt; pressing again shows the raw text. */
    private void cleanUp() {
        if (cleaning) return;
        if (cleaned) {   // toggle back to the raw text
            cleaned = false; text = raw; out.setText(raw);
            Ui.style(c, clean, Ui.Style.TONAL, Icon.Kind.REFRESH); clean.setText("Gemini'ye gönder (temizle)");
            return;
        }
        cleaning = true;
        clean.setText("Temizleniyor…"); clean.setEnabled(false);
        c.io.execute(() -> {
            String res = null, err = null;
            try { res = Ocr.cleanGeneral(c.host, raw); if (res.isEmpty()) err = "Boş yanıt"; }
            catch (Exception e) { err = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
            final String r = res, e = err;
            c.ui.post(() -> {
                cleaning = false;
                if (!clean.isAttachedToWindow()) return;
                clean.setEnabled(true);
                if (e != null) { clean.setText("Gemini'ye gönder (temizle)"); android.widget.Toast.makeText(c.a, e, android.widget.Toast.LENGTH_LONG).show(); return; }
                cleaned = true; text = r; out.setText(r);
                clean.setText("Ham metni göster");
            });
        });
    }

    private void copy() {
        ClipData clip = ClipData.newPlainText("OCR", text);
        PersistableBundle extras = new PersistableBundle();
        extras.putBoolean("android.content.extra.IS_SENSITIVE", true);  // keeps the text out of the keyboard/clipboard preview
        clip.getDescription().setExtras(extras);
        ((ClipboardManager) c.a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(clip);
        Ui.haptic(copy, HapticFeedbackConstants.CONFIRM);
        copy.setText("Kopyalandı");
        Ui.style(c, copy, Ui.Style.PRIMARY, Icon.Kind.CHECK);
        copy.animate().scaleX(1.04f).scaleY(1.04f).setDuration(120).withEndAction(() -> copy.animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(Ui.POP).start()).start();
        c.ui.postDelayed(() -> {
            if (copy.isAttachedToWindow()) { copy.setText("Kopyala"); Ui.style(c, copy, Ui.Style.PRIMARY, Icon.Kind.COPY); }
        }, 1600);
    }

    private void back() { s.selected.clear(); c.nav.replace(new PickerScreen(s)); }

    @Override public boolean onBack() { back(); return true; }
}
