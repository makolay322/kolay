package app.kolay.payload.tools.weather;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

/** Open-Meteo geocoding + 7-day forecast. Blocking: call off the UI thread. */
final class Forecast {
    static final class Day { String date; int code; double min, max, rainChance, rainMm; }

    String place;
    double nowTemp; int nowCode;
    final Day[] days;

    private Forecast(int n) { days = new Day[n]; }

    static Forecast fetch(String query) throws Exception {
        JSONObject g = get("https://geocoding-api.open-meteo.com/v1/search?count=1&language=tr&format=json&name="
                + URLEncoder.encode(query, "UTF-8"));
        JSONArray res = g.optJSONArray("results");
        if (res == null || res.length() == 0) throw new Exception("Yer bulunamadı: " + query);
        JSONObject loc = res.getJSONObject(0);
        String admin = loc.optString("admin1", "");
        String place = loc.optString("name") + (admin.isEmpty() || admin.equals(loc.optString("name")) ? "" : ", " + admin)
                + (loc.optString("country").isEmpty() ? "" : " · " + loc.optString("country"));

        JSONObject w = get("https://api.open-meteo.com/v1/forecast?timezone=auto&forecast_days=7"
                + "&latitude=" + loc.getDouble("latitude") + "&longitude=" + loc.getDouble("longitude")
                + "&current=temperature_2m,weather_code"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum");
        JSONObject d = w.getJSONObject("daily");
        JSONArray time = d.getJSONArray("time");
        Forecast f = new Forecast(time.length());
        f.place = place;
        JSONObject cur = w.getJSONObject("current");
        f.nowTemp = cur.getDouble("temperature_2m"); f.nowCode = cur.getInt("weather_code");
        for (int i = 0; i < time.length(); i++) {
            Day x = new Day();
            x.date = time.getString(i);
            x.code = d.getJSONArray("weather_code").getInt(i);
            x.max = d.getJSONArray("temperature_2m_max").getDouble(i);
            x.min = d.getJSONArray("temperature_2m_min").getDouble(i);
            x.rainChance = d.getJSONArray("precipitation_probability_max").optDouble(i, 0);
            x.rainMm = d.getJSONArray("precipitation_sum").optDouble(i, 0);
            f.days[i] = x;
        }
        return f;
    }

    private static JSONObject get(String url) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        h.setConnectTimeout(15_000); h.setReadTimeout(20_000);
        try {
            if (h.getResponseCode() != 200) throw new Exception("Hava servisi yanıt vermedi (" + h.getResponseCode() + ")");
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(h.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            return new JSONObject(sb.toString());
        } finally { h.disconnect(); }
    }

    /** WMO weather interpretation code -> Turkish label. */
    static String describe(int code) {
        if (code == 0) return "Açık";
        if (code == 1) return "Çoğunlukla açık";
        if (code == 2) return "Parçalı bulutlu";
        if (code == 3) return "Kapalı";
        if (code == 45 || code == 48) return "Sisli";
        if (code >= 51 && code <= 57) return "Çisenti";
        if (code >= 61 && code <= 65) return code == 65 ? "Şiddetli yağmur" : "Yağmurlu";
        if (code == 66 || code == 67) return "Dondurucu yağmur";
        if (code >= 71 && code <= 77) return "Karlı";
        if (code >= 80 && code <= 82) return "Sağanak";
        if (code == 85 || code == 86) return "Kar sağanağı";
        if (code == 95) return "Gök gürültülü";
        if (code == 96 || code == 99) return "Dolu ihtimalli fırtına";
        return "—";
    }
}
