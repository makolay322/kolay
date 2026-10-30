package app.kolay.payload.tools.weather;

import app.kolay.payload.tools.Tool;
import app.kolay.payload.ui.Ctx;
import app.kolay.payload.ui.Icon;
import app.kolay.payload.ui.Screen;

/** Daily forecast and rain chance for a searched place (Open-Meteo, no API key). */
public final class WeatherTool implements Tool {
    @Override public String id() { return "weather"; }
    @Override public String title() { return "Hava Durumu"; }
    @Override public String subtitle() { return "Günlük tahmin, yağış ihtimali"; }
    @Override public Icon.Kind icon() { return Icon.Kind.REFRESH; }
    @Override public Screen open(Ctx c) { return new WeatherScreen(); }
}
