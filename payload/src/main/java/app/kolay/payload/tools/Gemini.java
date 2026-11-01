package app.kolay.payload.tools;

import app.kolay.payload.screens.SettingsScreen;
import app.kolay.sdk.Host;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Minimal Gemini calls (text) using the key from Settings. Nothing is logged or stored. */
public final class Gemini {
    private static final String MODEL = "gemini-flash-lite-latest";
    private Gemini() {}

    public static String generate(Host host, String prompt) throws Exception {
        return generate(host, prompt, 0.2);
    }

    /** Text in, text out. Low {@code temperature} for tasks that must stay faithful (verdicts, translation). */
    public static String generate(Host host, String prompt, double temperature) throws Exception {
        JSONArray parts = new JSONArray().put(new JSONObject().put("text", prompt));
        return call(host, parts, temperature, 8192, 120000);
    }

    private static String call(Host host, JSONArray parts, double temperature, int maxTokens, int readTimeoutMs) throws Exception {
        String key = host.secret(SettingsScreen.LLM_SECRET);
        if (key.isEmpty()) throw new IOException("Gemini API anahtarı girilmemiş (Ayarlar)");
        JSONObject body = new JSONObject()
            .put("contents", new JSONArray().put(new JSONObject().put("parts", parts)))
            .put("generationConfig", new JSONObject().put("temperature", temperature).put("maxOutputTokens", maxTokens));
        HttpURLConnection c = (HttpURLConnection) new URL("https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent").openConnection();
        c.setRequestMethod("POST"); c.setDoOutput(true); c.setConnectTimeout(10000); c.setReadTimeout(readTimeoutMs);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("x-goog-api-key", key);
        try (OutputStream o = c.getOutputStream()) { o.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
        int code = c.getResponseCode();
        InputStream is = code < 400 ? c.getInputStream() : c.getErrorStream();
        String resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        if (code >= 400) throw new IOException("Gemini HTTP " + code);
        JSONArray cand = new JSONObject(resp).optJSONArray("candidates");
        if (cand == null || cand.length() == 0) throw new IOException("Gemini yanıt vermedi (içerik engellenmiş olabilir)");
        JSONObject content = cand.getJSONObject(0).optJSONObject("content");
        if (content == null) throw new IOException("Gemini boş yanıt döndürdü");
        JSONArray out = content.getJSONArray("parts");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < out.length(); i++) sb.append(out.getJSONObject(i).optString("text"));
        return sb.toString().trim();
    }
}
