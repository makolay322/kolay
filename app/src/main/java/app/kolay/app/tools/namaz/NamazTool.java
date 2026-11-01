package app.kolay.app.tools.namaz;

import app.kolay.app.tools.Tool;
import app.kolay.app.ui.Ctx;
import app.kolay.app.ui.Icon;
import app.kolay.app.ui.Screen;

/** Prayer times for the current location, a home-screen widget and a "time is about to run out" reminder (the work happens in the shell). */
public final class NamazTool implements Tool {
    @Override public String id() { return "namaz"; }
    @Override public String title() { return "Namaz Vakitleri"; }
    @Override public String subtitle() { return "Vakit · widget · hatırlatıcı"; }
    @Override public Icon.Kind icon() { return Icon.Kind.CLOCK; }
    @Override public Screen open(Ctx c) { return new NamazScreen(); }
}
