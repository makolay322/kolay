package app.kolay.payload.tools.qr;

import app.kolay.payload.tools.Tool;
import app.kolay.payload.ui.Ctx;
import app.kolay.payload.ui.Icon;
import app.kolay.payload.ui.Screen;

/** Live camera QR reader. Also the target of the shell's quick-settings tile (see {@code QrTileService}). */
public final class QrTool implements Tool {
    /** Must match QrTileService.ACTION_QR in the shell. */
    public static final String ACTION_QR = "app.kolay.action.QR_SCAN";

    @Override public String id() { return "qr"; }
    @Override public String title() { return "QR Okuyucu"; }
    @Override public String subtitle() { return "Kamerayla anında oku"; }
    @Override public Icon.Kind icon() { return Icon.Kind.QR; }
    @Override public Screen open(Ctx c) { return new QrScreen(); }
    @Override public boolean compact() { return true; }
}
