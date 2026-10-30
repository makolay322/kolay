package app.kolay.payload.tools.ocr;

import app.kolay.payload.tools.Tool;
import app.kolay.payload.ui.Ctx;
import app.kolay.payload.ui.Icon;
import app.kolay.payload.ui.Screen;

/** German medical documents: photos in, cleaned and ordered plain text out. */
public final class OcrTool implements Tool {
    @Override public String id() { return "ocr"; }
    @Override public String title() { return "Tıbbi OCR"; }
    @Override public String subtitle() { return "Belge fotoğraflarından düz metin"; }
    @Override public Icon.Kind icon() { return Icon.Kind.SCAN; }
    @Override public Screen open(Ctx c) { return new PickerScreen(new Session()); }
}
