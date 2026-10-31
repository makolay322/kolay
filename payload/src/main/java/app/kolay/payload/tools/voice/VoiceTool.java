package app.kolay.payload.tools.voice;

import app.kolay.payload.tools.Tool;
import app.kolay.payload.ui.Ctx;
import app.kolay.payload.ui.Icon;
import app.kolay.payload.ui.Screen;

/** Voice note -> text (German, Turkish or mixed), with one-tap translation into other languages. */
public final class VoiceTool implements Tool {
    @Override public String id() { return "ses"; }
    @Override public String title() { return "Sesli Not"; }
    @Override public String subtitle() { return "Konuş → yazı, çeviri"; }
    @Override public Icon.Kind icon() { return Icon.Kind.MIC; }
    @Override public Screen open(Ctx c) { return new VoiceScreen(); }
}
