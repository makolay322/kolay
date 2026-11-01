package app.kolay.payload.tools.friday;

import app.kolay.payload.tools.Tool;
import app.kolay.payload.ui.Ctx;
import app.kolay.payload.ui.Icon;
import app.kolay.payload.ui.Screen;

/** Random Islamic-style square Friday card with a well-known hadith; half of the backgrounds are black themes. */
public final class FridayTool implements Tool {
    @Override public String id() { return "cuma"; }
    @Override public String title() { return "Cuma Mesajı"; }
    @Override public String subtitle() { return "Hadisli kare görsel"; }
    @Override public Icon.Kind icon() { return Icon.Kind.CRESCENT; }
    @Override public Screen open(Ctx c) { return new FridayScreen(); }
}
