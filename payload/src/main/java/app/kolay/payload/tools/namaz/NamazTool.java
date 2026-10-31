package app.kolay.payload.tools.namaz;

import app.kolay.payload.tools.Tool;
import app.kolay.payload.ui.Ctx;
import app.kolay.payload.ui.Icon;
import app.kolay.payload.ui.Screen;

/** Prayer times for the current location, a home-screen widget and a "time is about to run out" reminder (the work happens in the shell). */
public final class NamazTool implements Tool {
    @Override public String id() { return "namaz"; }
    @Override public String title() { return "Namaz Vakitleri"; }
    @Override public String subtitle() { return "Konuma göre · widget · hatırlatıcı"; }
    @Override public Icon.Kind icon() { return Icon.Kind.CLOCK; }
    @Override public Screen open(Ctx c) { return new NamazScreen(); }
}
