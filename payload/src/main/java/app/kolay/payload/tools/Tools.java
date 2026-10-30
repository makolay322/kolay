package app.kolay.payload.tools;

import app.kolay.payload.tools.compress.CompressTool;
import app.kolay.payload.tools.ocr.OcrTool;
import app.kolay.payload.tools.weather.WeatherTool;
import app.kolay.payload.tools.youtube.YoutubeTool;

/** The toolbox contents, in display order. Add a line here to add a tool. */
public final class Tools {
    private Tools() {}

    public static final Tool[] ALL = {
        new OcrTool(),
        new YoutubeTool(),
        new CompressTool(),
        new WeatherTool(),
    };
}
