package app.kolay.payload.tools.compress;

import app.kolay.payload.tools.Tool;
import app.kolay.payload.ui.Ctx;
import app.kolay.payload.ui.Icon;
import app.kolay.payload.ui.Screen;

/** Re-encodes one chosen photo as JPEG at 90% quality. */
public final class CompressTool implements Tool {
    @Override public String id() { return "compress"; }
    @Override public String title() { return "Fotoğraf Sıkıştır"; }
    @Override public String subtitle() { return "JPEG, %90 kalite"; }
    @Override public Icon.Kind icon() { return Icon.Kind.SCAN; }
    @Override public Screen open(Ctx c) { return new CompressScreen(); }
    @Override public boolean compact() { return true; }
}
