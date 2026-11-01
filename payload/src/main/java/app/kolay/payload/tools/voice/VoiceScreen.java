package app.kolay.payload.tools.voice;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import app.kolay.payload.tools.Gemini;
import app.kolay.payload.ui.*;

/**
 * Tap to record -> text appears live while speaking (each pause-delimited segment is transcribed as soon as it is finished; German,
 * Turkish or a mix, each part kept in the language it was spoken in) -> optional one-tap translation into another language, all on
 * one page. The audio exists only in memory and every segment is dropped right after sending.
 */
final class VoiceScreen extends Screen {
    private static final int REQ_MIC = 73;
    private static final String[] LANGS = {
        "Türkçe", "Almanca", "İngilizce", "Arapça", "Rusça", "Fransızca", "İspanyolca", "İtalyanca", "Farsça", "Ukraynaca", "Lehçe", "Romence" };

    private static final String TRANSCRIBE =
        "Bu ses kaydını kelimesi kelimesine yazıya dök. Konuşmacı Almanca ve Türkçe konuşuyor olabilir; iki dil aynı cümle içinde de karışabilir. "
        + "Her bölümü KONUŞULDUĞU dilde yaz: hiçbir şeyi çevirme, dili değiştirme, Almancayı Türkçeye veya tersine uyarlama. "
        + "Türkçe için doğru harfleri (ç ğ ı İ ö ş ü), Almanca için ä ö ü ß ve isimlerde büyük harf kullan. "
        + "Noktalama ve gerekirse paragraf ekle. 'ııı', 'ähm', 'yani şey' gibi dolgu sesleri ve kekelemeleri at; "
        + "anlamlı hiçbir kelimeyi atma, ekleme veya yorumlama. Ad, ilaç adı, tıbbi terim, sayı ve tarihleri özenle yaz. "
        + "Konuşma yoksa veya anlaşılmıyorsa yalnızca [ANLAŞILAMADI] yaz. Çıktı yalnızca düz metin olsun: başlık, açıklama veya markdown yazma.";

    private static final String TRANSLATE =
        "Aşağıdaki metni %s diline çevir. Metin tek bir dilde ya da karışık dillerde (ör. yarı Almanca yarı Türkçe) olabilir; "
        + "tamamını akıcı ve doğal biçimde hedef dilde ver. Zaten hedef dildeki kısımları olduğu gibi koru. "
        + "Anlam ekleme, çıkarma veya yorumlama; ad, sayı, tarih ve tıbbi terimleri doğru aktar. "
        + "Çıktı yalnızca çeviri olsun: açıklama, tırnak, başlık veya markdown yazma.\n\nMETİN:\n";

    private Recorder rec;
    private FrameLayout micBtn, ring;
    private ImageView micIcon;
    private View stopSquare;
    private Arc spinner;
    private TextView status;
    private EditText transcript;
    private LinearLayout resultBox, transBox;
    private TextView transLabel, transText, transStatus;
    private final TextView[] chips = new TextView[LANGS.length];
    private boolean recording, working, asked, destroyed;
    private int transSeq, pending;
    private boolean gotText;
    private String lastErr;
    private final java.util.concurrent.ExecutorService chunkQueue = java.util.concurrent.Executors.newSingleThreadExecutor();
    private String lastTranslation = "";

    @Override public View create() {
        Palette p = c.p;
        LinearLayout page = new LinearLayout(c.a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.addView(new Header(c, "Sesli Not", "Almanca · Türkçe · karışık", () -> c.nav.pop()).view);

        LinearLayout col = new LinearLayout(c.a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(c.dp(16), c.dp(8), c.dp(16), c.dp(28));

        // ---- recorder ----
        FrameLayout stage = new FrameLayout(c.a);
        ring = new FrameLayout(c.a);
        ring.setBackground(Ui.rounded(c, p.danger, 70));
        ring.setAlpha(0f);
        stage.addView(ring, new FrameLayout.LayoutParams(c.dp(140), c.dp(140), Gravity.CENTER));
        micBtn = new FrameLayout(c.a);
        micBtn.setBackground(Ui.rounded(c, p.primary, 48));
        micBtn.setElevation(p.dark ? 0 : c.dp(4));
        micBtn.setClickable(true); micBtn.setContentDescription("Kaydı başlat / durdur");
        micIcon = Ui.icon(c, Icon.Kind.MIC, p.onPrimary, 40);
        micBtn.addView(micIcon, new FrameLayout.LayoutParams(c.dp(40), c.dp(40), Gravity.CENTER));
        stopSquare = new View(c.a);
        stopSquare.setBackground(Ui.rounded(c, 0xFFFFFFFF, 6));
        stopSquare.setVisibility(View.GONE);
        micBtn.addView(stopSquare, new FrameLayout.LayoutParams(c.dp(30), c.dp(30), Gravity.CENTER));
        micBtn.setOnClickListener(v -> onMicTap());
        Ui.press(micBtn);
        stage.addView(micBtn, new FrameLayout.LayoutParams(c.dp(96), c.dp(96), Gravity.CENTER));
        col.addView(stage, Ui.lp(c.dp(140), c.dp(140)));

        LinearLayout statusRow = new LinearLayout(c.a);
        statusRow.setGravity(Gravity.CENTER);
        spinner = new Arc(c.a, p.primarySoft, p.primary, c.dp(3));
        spinner.setVisibility(View.GONE);
        statusRow.addView(spinner, Ui.lp(c.dp(22), c.dp(22), 0, 0, c.dp(10), 0));
        status = Ui.text(c, "Dokun ve konuş; yazı konuştukça belirir. Almanca, Türkçe ya da ikisi karışık.", 14, p.muted, 500);
        status.setGravity(Gravity.CENTER); status.setLineSpacing(0, 1.2f);
        statusRow.addView(status, new LinearLayout.LayoutParams(-2, -2));
        col.addView(statusRow, Ui.lp(-1, -2, 0, c.dp(12), 0, 0));

        // ---- transcript + translation (hidden until there is text) ----
        resultBox = new LinearLayout(c.a);
        resultBox.setOrientation(LinearLayout.VERTICAL);
        resultBox.setVisibility(View.GONE);

        resultBox.addView(label("METİN"), Ui.lp(-2, -2, c.dp(4), c.dp(24), 0, c.dp(8)));
        LinearLayout card = new LinearLayout(c.a);
        card.setPadding(c.dp(16), c.dp(14), c.dp(16), c.dp(14));
        Ui.surface(c, card, p.surface, 22, true);
        transcript = new EditText(c.a);
        transcript.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        transcript.setBackground(null); transcript.setPadding(0, 0, 0, 0);
        transcript.setTextColor(p.ink); transcript.setTextSize(16); transcript.setLineSpacing(0, 1.3f);
        transcript.setMinLines(3); transcript.setGravity(Gravity.TOP);
        transcript.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        card.addView(transcript, new LinearLayout.LayoutParams(-1, -2));
        resultBox.addView(card, Ui.lp(-1, -2));
        resultBox.addView(actions(() -> transcript.getText().toString()), Ui.lp(-1, -2, 0, c.dp(10), 0, 0));

        resultBox.addView(label("ÇEVİR"), Ui.lp(-2, -2, c.dp(4), c.dp(24), 0, c.dp(8)));
        HorizontalScrollView hs = new HorizontalScrollView(c.a);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chipRow = new LinearLayout(c.a);
        for (int i = 0; i < LANGS.length; i++) {
            final int idx = i;
            TextView ch = Ui.text(c, LANGS[i], 14, p.ink, 600);
            ch.setGravity(Gravity.CENTER); ch.setClickable(true);
            ch.setPadding(c.dp(16), 0, c.dp(16), 0);
            paintChip(ch, false);
            ch.setOnClickListener(v -> translate(idx));
            Ui.press(ch);
            chips[i] = ch;
            chipRow.addView(ch, Ui.lp(-2, c.dp(40), 0, 0, c.dp(8), 0));
        }
        hs.addView(chipRow);
        resultBox.addView(hs, Ui.lp(-1, -2));

        transBox = new LinearLayout(c.a);
        transBox.setOrientation(LinearLayout.VERTICAL);
        transBox.setVisibility(View.GONE);
        transStatus = Ui.text(c, "", 13, p.muted, 500);
        transBox.addView(transStatus, Ui.lp(-1, -2, c.dp(4), c.dp(14), 0, 0));
        LinearLayout tcard = new LinearLayout(c.a);
        tcard.setOrientation(LinearLayout.VERTICAL);
        tcard.setPadding(c.dp(16), c.dp(14), c.dp(16), c.dp(14));
        Ui.surface(c, tcard, p.surface, 22, true);
        transLabel = Ui.text(c, "", 12, p.muted, 600);
        transLabel.setLetterSpacing(0.1f);
        tcard.addView(transLabel);
        transText = Ui.text(c, "", 16, p.ink, 400);
        transText.setLineSpacing(0, 1.3f); transText.setTextIsSelectable(true);
        tcard.addView(transText, Ui.lp(-1, -2, 0, c.dp(8), 0, 0));
        transBox.addView(tcard, Ui.lp(-1, -2, 0, c.dp(8), 0, 0));
        transBox.addView(actions(() -> lastTranslation), Ui.lp(-1, -2, 0, c.dp(10), 0, 0));
        resultBox.addView(transBox, Ui.lp(-1, -2));

        col.addView(resultBox, Ui.lp(-1, -2));

        TextView note = Ui.text(c, "Kayıt cihazda saklanmaz. Konuştukça, duraklamalarda parça parça yazıya çevrilmek üzere buluta gönderilir ve hemen atılır. En fazla 5 dakika.", 12, p.muted, 400);
        note.setGravity(Gravity.CENTER); note.setLineSpacing(0, 1.2f);
        col.addView(note, Ui.lp(-1, -2, c.dp(8), c.dp(28), c.dp(8), 0));

        ScrollView sv = new ScrollView(c.a);
        sv.setVerticalScrollBarEnabled(false);
        sv.setFillViewport(true);
        sv.addView(col);
        page.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        return page;
    }

    private TextView label(String s) {
        TextView t = Ui.text(c, s, 12, c.p.muted, 600);
        t.setLetterSpacing(0.1f);
        return t;
    }

    private interface Src { String get(); }

    /** Copy + share row for a text that is read when tapped. */
    private View actions(Src src) {
        LinearLayout row = new LinearLayout(c.a);
        TextView copy = Ui.button(c, "Kopyala", Ui.Style.TONAL, Icon.Kind.COPY);
        TextView share = Ui.button(c, "Paylaş", Ui.Style.TONAL, Icon.Kind.SHARE);
        copy.setOnClickListener(v -> copy(copy, src.get()));
        share.setOnClickListener(v -> {
            Ui.haptic(v, HapticFeedbackConstants.VIRTUAL_KEY);
            c.a.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, src.get()), null));
        });
        LinearLayout.LayoutParams l1 = Ui.lp(0, c.dp(52), 0, 0, c.dp(10), 0); l1.weight = 1;
        LinearLayout.LayoutParams l2 = Ui.lp(0, c.dp(52)); l2.weight = 1;
        row.addView(copy, l1); row.addView(share, l2);
        return row;
    }

    private void copy(TextView btn, String text) {
        ClipData clip = ClipData.newPlainText("Sesli not", text);
        PersistableBundle extras = new PersistableBundle();
        extras.putBoolean("android.content.extra.IS_SENSITIVE", true);
        clip.getDescription().setExtras(extras);
        ((ClipboardManager) c.a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(clip);
        Ui.haptic(btn, HapticFeedbackConstants.CONFIRM);
        btn.setText("Kopyalandı");
        c.ui.postDelayed(() -> { if (btn.isAttachedToWindow()) btn.setText("Kopyala"); }, 1600);
    }

    private void paintChip(TextView ch, boolean on) {
        ch.setTextColor(on ? c.p.onPrimary : c.p.ink);
        ch.setBackground(Ui.rounded(c, on ? c.p.primary : c.p.surface2, 20));
    }

    // ---- recording ------------------------------------------------------------------------------

    private boolean shellHasMic() {
        try {
            PackageInfo pi = c.a.getPackageManager().getPackageInfo(c.a.getPackageName(), PackageManager.GET_PERMISSIONS);
            if (pi.requestedPermissions != null) for (String s : pi.requestedPermissions) if (Manifest.permission.RECORD_AUDIO.equals(s)) return true;
        } catch (Exception ignored) { }
        return false;
    }

    private boolean granted() { return c.a.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED; }

    private void onMicTap() {
        if (working) return;
        Ui.haptic(micBtn, HapticFeedbackConstants.VIRTUAL_KEY);
        if (recording) { stopAndTranscribe(); return; }
        if (!shellHasMic()) { say("Bu özellik için kabuk güncellemesi gerekli: Ayarlar → “Kabuğu indir ve yükle”.", true); return; }
        if (granted()) { startRecording(); return; }
        if (!asked || c.a.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            asked = true;
            c.a.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
        } else {
            say("Mikrofon izni kapalı. Açılan ayarlardan izin ver.", true);
            c.a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", c.a.getPackageName(), null)));
        }
    }

    @Override public void onPermission(int req, String[] perms, int[] results) {
        if (req != REQ_MIC) return;
        if (granted()) startRecording(); else say("Kayıt için mikrofon izni gerekli.", true);
    }

    private void startRecording() {
        Recorder r = new Recorder();
        try { r.start(); } catch (Exception e) { say(e.getMessage() == null ? "Mikrofon açılamadı" : e.getMessage(), true); return; }
        rec = r; recording = true;
        gotText = false; lastErr = null;
        transcript.setText("");
        resetTranslation();
        resultBox.setVisibility(View.GONE);
        micBtn.setBackground(Ui.rounded(c, c.p.danger, 48));
        micIcon.setVisibility(View.GONE); stopSquare.setVisibility(View.VISIBLE);
        say("0:00 · durdurmak için dokun", false);
        Ui.haptic(micBtn, HapticFeedbackConstants.CONFIRM);
        tick();
    }

    /** Timer text, pulse ring following the voice level, and the 5-minute stop. */
    private void tick() {
        if (!recording || destroyed || rec == null) return;
        float lv = rec.level();
        ring.animate().cancel();
        ring.setAlpha(0.18f + 0.22f * lv);
        float s = 0.72f + 0.28f * lv;
        ring.animate().scaleX(s).scaleY(s).setDuration(120).start();
        long sec = rec.millis() / 1000;
        status.setText(String.format("%d:%02d · %s", sec / 60, sec % 60, gotText || pending > 0 ? "canlı yazıya çevriliyor · durdurmak için dokun" : "durdurmak için dokun"));
        for (byte[] w; (w = rec.poll()) != null; ) submit(w);
        if (rec.limitHit()) { stopAndTranscribe(); return; }
        c.ui.postDelayed(this::tick, 120);
    }

    private void stopAndTranscribe() {
        if (!recording || rec == null) return;
        recording = false;
        Recorder r = rec; rec = null;
        boolean silent = r.silent();
        r.stop();
        for (byte[] w; (w = r.poll()) != null; ) submit(w);
        micBtn.setBackground(Ui.rounded(c, c.p.primary, 48));
        micIcon.setVisibility(View.VISIBLE); stopSquare.setVisibility(View.GONE);
        ring.animate().cancel(); ring.animate().alpha(0f).setDuration(200).start();
        if (pending == 0 && !gotText) {
            say(silent ? "Ses algılanmadı. Mikrofona biraz daha yakın konuş." : "Konuşma anlaşılamadı. Tekrar dene.", true);
            return;
        }
        working = true;
        micBtn.setAlpha(0.5f);
        spinner.setVisibility(View.VISIBLE);
        say("Son kısım yazıya çevriliyor…", false);
        if (pending == 0) finishTranscription();
    }

    /** Queues one finished speech segment; the single-thread queue keeps the text in speaking order. */
    private void submit(byte[] wav) {
        pending++;
        chunkQueue.execute(() -> {
            String out = null, err = null;
            try { out = Gemini.generateFromAudio(c.host, TRANSCRIBE, wav, "audio/wav"); }
            catch (Exception e) { err = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
            final String o = out, e = err;
            c.ui.post(() -> { if (!destroyed) chunkDone(o, e); });
        });
    }

    private void chunkDone(String text, String err) {
        pending--;
        if (err != null) lastErr = err;
        else if (text != null) {
            String t = text.trim();
            if (!t.isEmpty() && !t.contains("[ANLAŞILAMADI]")) {
                if (transcript.length() > 0) transcript.append(" ");
                transcript.append(t);
                gotText = true;
                if (resultBox.getVisibility() != View.VISIBLE) { resultBox.setVisibility(View.VISIBLE); Ui.appear(resultBox, 0); }
                if (lastTranslation.length() > 0) resetTranslation();    // the source text changed
            }
        }
        if (!recording && pending == 0) finishTranscription();
    }

    private void finishTranscription() {
        working = false; micBtn.setAlpha(1f); spinner.setVisibility(View.GONE);
        if (!gotText) { say(lastErr != null ? "Hata: " + lastErr : "Konuşma anlaşılamadı. Tekrar dene.", true); return; }
        say(lastErr != null ? "Hazır, ama bazı kısımlar çevrilemedi (" + lastErr + "). Metni düzeltebilirsin." : "Hazır. Metni düzeltebilir, çevirebilirsin.", lastErr != null);
        Ui.haptic(micBtn, HapticFeedbackConstants.CONFIRM);
    }

    private void say(String s, boolean error) {
        status.setTextColor(error ? c.p.danger : c.p.muted);
        if (recording) status.setText(s); else Ui.swapText(status, s);
    }

    // ---- translation ----------------------------------------------------------------------------

    private void resetTranslation() {
        transSeq++;
        lastTranslation = "";
        transBox.setVisibility(View.GONE);
        for (TextView ch : chips) paintChip(ch, false);
    }

    private void translate(int idx) {
        String src = transcript.getText().toString().trim();
        if (src.isEmpty()) return;
        Ui.haptic(chips[idx], HapticFeedbackConstants.VIRTUAL_KEY);
        for (int i = 0; i < chips.length; i++) paintChip(chips[i], i == idx);
        final int seq = ++transSeq;
        String lang = LANGS[idx];
        transBox.setVisibility(View.VISIBLE);
        transLabel.setText(lang.toUpperCase(new java.util.Locale("tr")));
        transText.setText("");
        transStatus.setTextColor(c.p.muted);
        transStatus.setText("Çevriliyor…");
        c.io.execute(() -> {
            String out = null, err = null;
            try { out = Gemini.generate(c.host, String.format(TRANSLATE, lang) + src, 0.1); }
            catch (Exception e) { err = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
            final String o = out, e = err;
            c.ui.post(() -> {
                if (destroyed || seq != transSeq) return;      // another language was picked meanwhile
                if (e != null || o == null || o.isEmpty()) {
                    transStatus.setTextColor(c.p.danger);
                    transStatus.setText("Hata: " + (e == null ? "çeviri üretilemedi" : e));
                    return;
                }
                lastTranslation = o;
                transStatus.setText("");
                transText.setText(o);
            });
        });
    }

    // ---- lifecycle ------------------------------------------------------------------------------

    /** Leaving the app mid-recording must not leave the microphone open: finish the clip and transcribe it. */
    @Override public void onPause() { if (recording) stopAndTranscribe(); }

    @Override public void onDestroy() {
        destroyed = true;
        chunkQueue.shutdownNow();
        if (rec != null) { rec.discard(); rec = null; }
        recording = false;
    }

    @Override public boolean onBack() {
        if (recording) { stopAndTranscribe(); return true; }
        return false;
    }
}
