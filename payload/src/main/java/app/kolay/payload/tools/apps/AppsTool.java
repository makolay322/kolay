package app.kolay.payload.tools.apps;

import app.kolay.payload.tools.Tool;
import app.kolay.payload.ui.Ctx;
import app.kolay.payload.ui.Icon;
import app.kolay.payload.ui.Screen;

/** Updates apps installed from outside Google Play from APKPure, APKMirror or LiteAPKs (chosen per app). */
public final class AppsTool implements Tool {
    @Override public String id() { return "apps"; }
    @Override public String title() { return "Uygulama Güncelle"; }
    @Override public String subtitle() { return "Play dışı APK'lar"; }
    @Override public Icon.Kind icon() { return Icon.Kind.APPS; }
    @Override public Screen open(Ctx c) { return new AppsScreen(); }
}
