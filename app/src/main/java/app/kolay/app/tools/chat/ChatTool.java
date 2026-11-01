package app.kolay.app.tools.chat;

import app.kolay.app.tools.Tool;
import app.kolay.app.ui.Ctx;
import app.kolay.app.ui.Icon;
import app.kolay.app.ui.Screen;

/** A small chat box for quick questions and short text jobs, using the key from Settings. Nothing is stored. */
public final class ChatTool implements Tool {
    @Override public String id() { return "chat"; }
    @Override public String title() { return "Sohbet"; }
    @Override public String subtitle() { return "Kısa sorular, hızlı işler"; }
    @Override public Icon.Kind icon() { return Icon.Kind.CHAT; }
    @Override public Screen open(Ctx c) { return new ChatScreen(); }
}
