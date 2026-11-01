package app.kolay.app.tools.apps;

import app.kolay.app.tools.Tool;
import app.kolay.app.ui.Ctx;
import app.kolay.app.ui.Icon;
import app.kolay.app.ui.Screen;

/** Updates apps installed from outside Google Play from APKPure, APKMirror or LiteAPKs (chosen per app). */
public final class AppsTool implements Tool {
    @Override public String id() { return "apps"; }
    @Override public String title() { return "Uygulama Güncelle"; }
    @Override public String subtitle() { return "Play dışı APK'lar"; }
    @Override public Icon.Kind icon() { return Icon.Kind.APPS; }
    @Override public Screen open(Ctx c) { return new AppsScreen(); }
}
