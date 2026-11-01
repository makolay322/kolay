package app.kolay.payload.tools.ocr;

import app.kolay.payload.tools.Tool;
import app.kolay.payload.ui.Ctx;
import app.kolay.payload.ui.Icon;
import app.kolay.payload.ui.Screen;

/** Photos in, plain text out: medical mode (German medical documents, masked and cleaned) or general mode (local OCR, optional cleanup). */
public final class OcrTool implements Tool {
    @Override public String id() { return "ocr"; }
    @Override public String title() { return "Tıbbi OCR"; }
    @Override public String subtitle() { return "Tıbbi belge · genel metin okuma"; }
    @Override public Icon.Kind icon() { return Icon.Kind.SCAN; }
    @Override public Screen open(Ctx c) { return new PickerScreen(new Session()); }
}
