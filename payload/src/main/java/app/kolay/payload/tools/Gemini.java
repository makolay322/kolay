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

/** Minimal Gemini text-in/text-out call using the key from Settings. Nothing is logged or stored. */
public final class Gemini {
    private static final String MODEL = "gemini-flash-lite-latest";
    private Gemini() {}

    public static String generate(Host host, String prompt) throws Exception {
        String key = host.secret(SettingsScreen.LLM_SECRET);
        if (key.isEmpty()) throw new IOException("Gemini API anahtarı girilmemiş (Ayarlar)");
        JSONObject body = new JSONObject()
            .put("contents", new JSONArray().put(new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", prompt)))))
            .put("generationConfig", new JSONObject().put("temperature", 0.2).put("maxOutputTokens", 8192));
        HttpURLConnection c = (HttpURLConnection) new URL("https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent").openConnection();
        c.setRequestMethod("POST"); c.setDoOutput(true); c.setConnectTimeout(10000); c.setReadTimeout(120000);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("x-goog-api-key", key);
        try (OutputStream o = c.getOutputStream()) { o.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
        int code = c.getResponseCode();
        InputStream is = code < 400 ? c.getInputStream() : c.getErrorStream();
        String resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        if (code >= 400) throw new IOException("Gemini HTTP " + code);
        JSONArray parts = new JSONObject(resp).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) sb.append(parts.getJSONObject(i).optString("text"));
        return sb.toString().trim();
    }
}
