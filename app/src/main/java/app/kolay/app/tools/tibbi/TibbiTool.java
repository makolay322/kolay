package app.kolay.app.tools.tibbi;

import app.kolay.app.tools.Tool;
import app.kolay.app.ui.Ctx;
import app.kolay.app.ui.Icon;
import app.kolay.app.ui.Screen;

/** Offline clinical calculators (eGFR, QTc, CHA₂DS₂-VA…). Nothing typed is stored. */
public final class TibbiTool implements Tool {
    @Override public String id() { return "tibbi"; }
    @Override public String title() { return "Tıbbi Hesap"; }
    @Override public String subtitle() { return "Skorlar ve formüller"; }
    @Override public Icon.Kind icon() { return Icon.Kind.PULSE; }
    @Override public Screen open(Ctx c) { return new TibbiScreen(); }
}
