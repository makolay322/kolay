package app.kolay.payload.tools.ocr;

import app.kolay.payload.screens.SettingsScreen;
import app.kolay.sdk.Host;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.net.Uri;
import com.googlecode.tesseract.android.TessBaseAPI;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;

/** Photos -> Tesseract (deu) -> local PII masking -> one LLM request (page order + cleanup) -> plain text. Nothing is written to disk. */
final class Ocr {
    private static final int MAX_SIDE = 2400;
    private static final String MODEL = "gemini-flash-lite-latest";
    private static final String PROMPT =
        "Aşağıda, Almanca bir tıbbi belgenin (rapor, epikriz, bulgu, reçete, faks vb.) sayfalarının Tesseract OCR çıktıları var. "
        + "Sayfalar KARIŞIK sırada gönderilmiş olabilir; her biri '=== SAYFA n ===' satırıyla başlar. "
        + "Görevin: (1) Sayfaları doğru sıraya diz (sayfa numaraları, 'Seite x von y', içerik akışı, cümlelerin sayfa sonunda/başında devam etmesi, "
        + "başlık/bitiş imzası gibi ipuçlarıyla). Bunu yaparken yalnızca sayfaların SIRASINI değiştir; sayfaları bölme, birleştirme, "
        + "yeniden yazma veya tekrar etme, her sayfa çıktıda tam bir kez yer alsın. "
        + "(2) Her sayfada OCR hatalarını tıbbi bağlamı kullanarak düzelt (bozuk kelimeler, yanlış harf/rakamlar, tıbbi terimler, ilaç adları, "
        + "birimler). "
        + "(3) Hasta brifinine katkısı olmayan yerleri ÇIKAR: "
        + "(a) Gönderen kurumun (hastane, klinik, muayenehane, laboratuvar) iletişim ve kurumsal bilgileri: telefon, faks, e-posta, web adresi, "
        + "sokak/posta kodu adresi, banka/IBAN, vergi ve kurum numarası, çalışma saatleri, ulaşım tarifi, logo/antet metni. "
        + "Kurumun ve bölümün ADI ile sorumlu hekimin adı ve unvanı kalsın (belgenin kimden geldiğini gösterir) ama yalnızca ilk geçtiği yerde. "
        + "(b) Her sayfada yinelenen üst/alt bilgiler: antet, 'Seite x von y', sayfa numarası, tekrar eden hasta/dosya başlığı satırları, "
        + "faks başlık/altlık satırları (gönderim tarihi, faks numarası), basım/oluşturma damgaları, barkod ve belge numarası döküntüleri. "
        + "(c) Yasal/idari kalıp metinler: gizlilik uyarısı, 'Dieses Schreiben ist vertraulich…' türü ibareler, veri koruma notları, "
        + "'Mit freundlichen kollegialen Grüßen' gibi kapanış nezaket kalıpları ve boş imza çizgileri (imzalayan hekimin adı unvanı kalsın). "
        + "(d) Aynen tekrar eden satır/paragraflar (aynı cümlenin iki kez yazılması, sayfa sonunda tekrarlanan satırlar): yalnızca ilkini bırak. "
        + "(e) Anlamsız çöp: rastgele semboller, boş yer tutucular, tablo çizgisi artıkları, gürültü. "
        + "KESİN KURALLAR: Tıbbi içeriğin hiçbir parçasını çıkarma veya özetleme; tüm cümleler, bulgular, tanılar, ICD kodları, ilaçlar, dozlar, "
        + "laboratuvar değerleri, referans aralıkları, tarihler (muayene, yatış, taburcu, kontrol), ölçümler, öneriler ve kontrol randevuları kalmalı. "
        + "Yinelenen tekrar ile özdeş görünen ama farklı tarih/değer taşıyan satırları (örn. farklı günlerin laboratuvar sonuçları) ASLA birleştirme. "
        + "Emin değilsen metni olduğu gibi bırak. Hiçbir bilgi ekleme veya uydurma. "
        + "[NAME] ile gösterilen sansürlü yerleri aynen koru. Dili Almanca bırak, çevirme. Satır yapısını mümkün olduğunca koru. "
        + "Çıktı yalnızca düz metin olsun: sayfa başlığı ('=== SAYFA n ===' dahil), açıklama, markdown veya kod bloğu yazma; "
        + "sayfaları yalnızca bir boş satırla ayır.\n\nSAYFALAR:\n";

    private final Context ctx;
    private final Host host;
    private TessBaseAPI tess;

    Ocr(Host h) { host = h; ctx = h.activity().getApplicationContext(); }

    private synchronized TessBaseAPI tess() throws IOException {
        if (tess != null) return tess;
        TessBaseAPI a = new TessBaseAPI();
        if (!a.init(host.tessDataParent().getAbsolutePath(), "deu")) throw new IOException("Tesseract başlatılamadı");
        a.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_COLUMN);
        a.setVariable("user_defined_dpi", "300");
        return tess = a;
    }

    /** Gri tonlama + kontrast germe; EXIF döndürme ImageDecoder'da otomatik. */
    private Bitmap load(Uri uri) throws IOException {
        Bitmap bmp = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.getContentResolver(), uri), (d, info, s) -> {
            int m = Math.max(info.getSize().getWidth(), info.getSize().getHeight());
            if (m > MAX_SIDE) d.setTargetSampleSize((int) Math.ceil(m / (double) MAX_SIDE));
            d.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
        });
        Bitmap g = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
        ColorMatrix cm = new ColorMatrix(); cm.setSaturation(0);
        float c = 1.25f, off = -(0.125f * 255);  // hafif kontrast artışı
        cm.postConcat(new ColorMatrix(new float[]{c,0,0,0,off, 0,c,0,0,off, 0,0,c,0,off, 0,0,0,1,0}));
        Paint p = new Paint(); p.setColorFilter(new ColorMatrixColorFilter(cm));
        new Canvas(g).drawBitmap(bmp, 0, 0, p);
        bmp.recycle();
        return g;
    }

    interface Progress { void on(String status, float fraction); }

    private volatile boolean cancelled;
    void cancel() { cancelled = true; }

    synchronized void close() { if (tess != null) { tess.recycle(); tess = null; } }

    /** Tüm sayfalar: her biri Tesseract + sansür, ardından tek Gemini isteği (sıralama + düzeltme). */
    synchronized String run(java.util.List<Uri> uris, Progress pr) throws Exception {
        StringBuilder all = new StringBuilder(), plain = new StringBuilder();
        TessBaseAPI a = tess();
        java.util.List<String> raws = new java.util.ArrayList<>();
        for (int i = 0; i < uris.size(); i++) {
            if (cancelled) throw new InterruptedIOException("iptal");
            pr.on("Sayfa " + (i + 1) + "/" + uris.size() + " okunuyor…", 0.08f + 0.62f * i / uris.size());
            Bitmap bmp = load(uris.get(i));
            a.setImage(bmp);
            String raw = a.getUTF8Text();
            bmp.recycle();
            raws.add(raw == null ? "" : raw);
        }
        java.util.Set<String> known = Mask.collect(raws);  // bir sayfada bulunan isim tüm sayfalarda sansürlenir
        for (int i = 0; i < raws.size(); i++) {
            String masked = Mask.apply(raws.get(i), known).trim();
            if (masked.isEmpty()) continue;
            all.append("=== SAYFA ").append(i + 1).append(" ===\n").append(masked).append("\n\n");
            plain.append(masked).append("\n\n");
        }
        if (all.length() == 0) return "";
        if (cancelled) throw new InterruptedIOException("iptal");
        pr.on("Metin cilalanıyor…", 0.78f);
        try { return gemini(all.toString()); }
        catch (Exception e) { return plain.toString().trim() + "\n\n[Cilalama başarısız: " + e.getMessage() + " — ham OCR gösteriliyor]"; }
    }

    /** General mode: Tesseract only, automatic page layout, no masking and no network. Pages are joined with a blank line. */
    synchronized String runPlain(java.util.List<Uri> uris, Progress pr) throws Exception {
        TessBaseAPI a = tess();
        a.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
        StringBuilder out = new StringBuilder();
        try {
            for (int i = 0; i < uris.size(); i++) {
                if (cancelled) throw new InterruptedIOException("iptal");
                pr.on("Sayfa " + (i + 1) + "/" + uris.size() + " okunuyor…", 0.08f + 0.9f * i / uris.size());
                Bitmap bmp = load(uris.get(i));
                a.setImage(bmp);
                String raw = a.getUTF8Text();
                bmp.recycle();
                if (raw != null && !raw.trim().isEmpty()) out.append(raw.trim()).append("\n\n");
            }
        } finally {
            a.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_COLUMN);   // the medical mode relies on the default set in tess()
        }
        return out.toString().trim();
    }

    private static final String GENERAL_PROMPT =
        "Aşağıdaki metin bir fotoğraftan Tesseract OCR ile okundu (Almanca, Türkçe veya İngilizce olabilir). "
        + "OCR hatalarını düzelt (bozuk harf ve kelimeler, yanlış birleşmiş veya bölünmüş kelimeler, satır sonu kırıkları), "
        + "paragraf ve satır düzenini okunur hale getir, çöp karakterleri ve anlamsız tekrarları at. "
        + "Anlamı değiştirme; bilgi ekleme, özetleme veya çevirme. Dil ve içerik aynı kalsın. "
        + "Çıktı yalnızca düzeltilmiş düz metin olsun; açıklama, markdown veya kod bloğu yazma.\n\nMETİN:\n";

    /** General mode, on request only: one cleanup request with a loose prompt. Throws on a missing key or network error. */
    static String cleanGeneral(Host host, String text) throws Exception {
        // same on-device masking as the medical mode: names, birth dates, addresses and IBANs never leave the phone
        String masked = Mask.apply(text, Mask.collect(java.util.Collections.singletonList(text))).trim();
        return app.kolay.payload.tools.Gemini.generate(host, GENERAL_PROMPT + masked, 0.2).trim();
    }

    private String gemini(String text) throws Exception {
        String key = host.secret(SettingsScreen.LLM_SECRET);
        if (key.isEmpty()) throw new IOException("API anahtarı girilmemiş (Ayarlar)");
        JSONObject body = new JSONObject()
            .put("contents", new JSONArray().put(new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", PROMPT + text)))))
            .put("generationConfig", new JSONObject().put("temperature", 0).put("maxOutputTokens", 32768));
        HttpURLConnection c = (HttpURLConnection) new URL("https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent").openConnection();
        c.setRequestMethod("POST"); c.setDoOutput(true); c.setConnectTimeout(10000); c.setReadTimeout(60000);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("x-goog-api-key", key);
        try (OutputStream o = c.getOutputStream()) { o.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
        int code = c.getResponseCode();
        InputStream is = code < 400 ? c.getInputStream() : c.getErrorStream();
        String resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        if (code >= 400) throw new IOException("HTTP " + code);
        JSONArray parts = new JSONObject(resp).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) sb.append(parts.getJSONObject(i).optString("text"));
        return sb.toString().trim();
    }
}
