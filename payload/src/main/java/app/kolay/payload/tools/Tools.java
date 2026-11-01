package app.kolay.payload.tools;

import app.kolay.payload.tools.apps.AppsTool;
import app.kolay.payload.tools.compress.CompressTool;
import app.kolay.payload.tools.friday.FridayTool;
import app.kolay.payload.tools.halal.HelalTool;
import app.kolay.payload.tools.namaz.NamazTool;
import app.kolay.payload.tools.ocr.OcrTool;
import app.kolay.payload.tools.qr.QrTool;
import app.kolay.payload.tools.youtube.YoutubeTool;

/** The toolbox contents, in display order. Add a line here to add a tool. */
public final class Tools {
    private Tools() {}

    public static final Tool[] ALL = {
        new OcrTool(),
        new YoutubeTool(),
        new CompressTool(),
        new QrTool(),
        new HelalTool(),
        new FridayTool(),
        new AppsTool(),
        new NamazTool(),
    };
}
