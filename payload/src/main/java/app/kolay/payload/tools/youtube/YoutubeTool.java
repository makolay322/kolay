package app.kolay.payload.tools.youtube;

import app.kolay.payload.tools.Tool;
import app.kolay.payload.ui.Ctx;
import app.kolay.payload.ui.Icon;
import app.kolay.payload.ui.Screen;

/** A pasted YouTube link: original-language transcript in, Turkish summary out. */
public final class YoutubeTool implements Tool {
    @Override public String id() { return "youtube"; }
    @Override public String title() { return "YouTube Özeti"; }
    @Override public String subtitle() { return "Bağlantıdan Türkçe özet"; }
    @Override public Icon.Kind icon() { return Icon.Kind.PLAY; }
    @Override public Screen open(Ctx c) { return new YoutubeScreen(); }
    @Override public boolean compact() { return true; }

    /** Opened from the system share sheet ("YouTube Özet"): the shared text is filled in and the summary starts at once. */
    public Screen openShared(Ctx c, String sharedText) { return new YoutubeScreen(sharedText); }
}
