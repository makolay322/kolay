package app.kolay.payload.tools.youtube;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import app.kolay.payload.tools.Gemini;
import app.kolay.payload.ui.*;

/** Link field -> transcript (original language) -> Gemini Turkish summary, all on one screen. Nothing is stored. */
final class YoutubeScreen extends Screen {
    private static final int MAX_CHARS = 400_000;
    private static final String PROMPT =
        "Aşağıda bir YouTube videosunun transkripti var (dil: %s). Videoyu TÜRKÇE özetle. "
        + "Önce 1-2 cümlelik genel özet, ardından ana noktaları madde madde yaz; önemli sayı, isim ve sonuçları koru. "
        + "Transkriptte olmayan hiçbir bilgi ekleme. Çıktı düz metin olsun, markdown başlığı veya kod bloğu kullanma.\n\nTRANSKRİPT:\n";

    private EditText link;
    private TextView go, status, result;
    private View resultCard, actions;
    private volatile boolean destroyed;
    private boolean busy;
    private String summary = "";
    private final String shared;   // text received from the share sheet, or null

    YoutubeScreen() { this(null); }
    YoutubeScreen(String shared) { this.shared = shared; }

    @Override public View create() {
        Palette p = c.p;
        LinearLayout page = new LinearLayout(c.a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.addView(new Header(c, "YouTube Özeti", "Bağlantıyı yapıştır", () -> c.nav.pop()).view);

        LinearLayout col = new LinearLayout(c.a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(c.dp(16), c.dp(6), c.dp(16), c.dp(24));

        link = new EditText(c.a);
        link.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        link.setSingleLine(true); link.setHint("https://youtu.be/…");
        link.setTextColor(p.ink); link.setHintTextColor(p.muted); link.setTextSize(15);
        link.setBackground(Ui.rounded(c, p.surface2, 14));
        link.setPadding(c.dp(14), c.dp(13), c.dp(14), c.dp(13));
        link.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        col.addView(link, Ui.lp(-1, -2));

        LinearLayout row = new LinearLayout(c.a);
        TextView paste = Ui.button(c, "Yapıştır", Ui.Style.TONAL, Icon.Kind.COPY);
        go = Ui.button(c, "Özetle", Ui.Style.PRIMARY, Icon.Kind.CHECK);
        paste.setOnClickListener(v -> pasteFromClipboard());
        go.setOnClickListener(v -> start());
        row.addView(paste, Ui.lp(0, c.dp(52), 0, 0, c.dp(10), 0));
        ((LinearLayout.LayoutParams) paste.getLayoutParams()).weight = 1;
        row.addView(go, Ui.lp(0, c.dp(52)));
        ((LinearLayout.LayoutParams) go.getLayoutParams()).weight = 1;
        col.addView(row, Ui.lp(-1, -2, 0, c.dp(12), 0, 0));

        status = Ui.text(c, "", 13, p.muted, 500);
        status.setVisibility(View.GONE);
        col.addView(status, Ui.lp(-1, -2, c.dp(4), c.dp(16), 0, 0));

        result = Ui.text(c, "", 15, p.ink, 400);
        result.setLineSpacing(0, 1.3f);
        LinearLayout card = new LinearLayout(c.a);
        card.setPadding(c.dp(18), c.dp(18), c.dp(18), c.dp(18));
        Ui.surface(c, card, p.surface, 22, true);
        card.addView(result);
        card.setVisibility(View.GONE);
        resultCard = card;
        col.addView(card, Ui.lp(-1, -2, 0, c.dp(16), 0, 0));

        LinearLayout act = new LinearLayout(c.a);
        TextView copy = Ui.button(c, "Kopyala", Ui.Style.TONAL, Icon.Kind.COPY);
        TextView share = Ui.button(c, "Paylaş", Ui.Style.TONAL, Icon.Kind.SHARE);
        copy.setOnClickListener(v -> copy(v));
        share.setOnClickListener(v -> c.a.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, summary), null)));
        act.addView(copy, Ui.lp(0, c.dp(52), 0, 0, c.dp(10), 0));
        ((LinearLayout.LayoutParams) copy.getLayoutParams()).weight = 1;
        act.addView(share, Ui.lp(0, c.dp(52)));
        ((LinearLayout.LayoutParams) share.getLayoutParams()).weight = 1;
        act.setVisibility(View.GONE);
        actions = act;
        col.addView(act, Ui.lp(-1, -2, 0, c.dp(12), 0, 0));

        ScrollView sv = new ScrollView(c.a);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(col);
        page.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        return page;
    }

    private void pasteFromClipboard() {
        ClipData d = ((ClipboardManager) c.a.getSystemService(Context.CLIPBOARD_SERVICE)).getPrimaryClip();
        if (d != null && d.getItemCount() > 0 && d.getItemAt(0).getText() != null) {
            link.setText(d.getItemAt(0).getText().toString().trim());
            link.setSelection(link.length());
        }
    }

    private void start() {
        if (busy) return;
        String id = Transcript.videoId(link.getText().toString());
        if (id == null) { Toast.makeText(c.a, "Geçerli bir YouTube bağlantısı yapıştır", Toast.LENGTH_SHORT).show(); return; }
        busy = true;
        Ui.haptic(go, HapticFeedbackConstants.VIRTUAL_KEY);
        go.setEnabled(false);
        resultCard.setVisibility(View.GONE); actions.setVisibility(View.GONE);
        status.setVisibility(View.VISIBLE); status.setTextColor(c.p.muted);
        Ui.swapText(status, "Transkript alınıyor…");
        c.io.execute(() -> {
            String out = null, err = null;
            try {
                Transcript.Result t = Transcript.fetch(id);
                post(() -> Ui.swapText(status, "Özetleniyor… (" + t.lang + " transkript)"));
                String text = t.text.length() > MAX_CHARS ? t.text.substring(0, MAX_CHARS) : t.text;
                out = Gemini.generate(c.host, String.format(PROMPT, t.lang) + text);
                if (out.isEmpty()) err = "Özet üretilemedi";
            } catch (Exception e) { err = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
            final String o = out, e = err;
            post(() -> done(o, e));
        });
    }

    private void done(String out, String err) {
        busy = false; go.setEnabled(true);
        if (err != null) { status.setTextColor(c.p.danger); Ui.swapText(status, "Hata: " + err); return; }
        summary = out;
        status.setVisibility(View.GONE);
        result.setText(out);
        resultCard.setVisibility(View.VISIBLE); actions.setVisibility(View.VISIBLE);
        Ui.appear(resultCard, 0); Ui.appear(actions, 80);
        Ui.haptic(go, HapticFeedbackConstants.CONFIRM);
    }

    private void post(Runnable r) { c.ui.post(() -> { if (!destroyed) r.run(); }); }

    private void copy(View v) {
        ClipData clip = ClipData.newPlainText("Özet", summary);
        ((ClipboardManager) c.a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(clip);
        Ui.haptic(v, HapticFeedbackConstants.CONFIRM);
        Toast.makeText(c.a, "Kopyalandı", Toast.LENGTH_SHORT).show();
    }

    @Override public void onShown() {
        if (shared == null) return;
        link.setText(shared.trim());
        link.setSelection(link.length());
        if (Transcript.videoId(shared) != null) start();
    }

    @Override public void onDestroy() { destroyed = true; }
}
