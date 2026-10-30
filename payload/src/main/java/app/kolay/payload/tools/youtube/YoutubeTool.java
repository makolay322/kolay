package app.kolay.payload.tools.youtube;

import app.kolay.payload.tools.Tool;
import app.kolay.payload.ui.Ctx;
import app.kolay.payload.ui.Icon;
import app.kolay.payload.ui.Screen;

/** A pasted YouTube link: original-language transcript in, Turkish summary out. */
public final class YoutubeTool implements Tool {
    @Override public String id() { return "youtube"; }
    @Override public String title() { return "YouTube Özeti"; }
    @Override public String subtitle() { return "Video bağlantısından Türkçe özet"; }
    @Override public Icon.Kind icon() { return Icon.Kind.LINK; }
    @Override public Screen open(Ctx c) { return new YoutubeScreen(); }
}
