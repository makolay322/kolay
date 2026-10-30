package app.kolay.payload.tools;

import app.kolay.payload.ui.Ctx;
import app.kolay.payload.ui.Icon;
import app.kolay.payload.ui.Screen;

/** One entry in the toolbox. Register new tools in {@link Tools}. */
public interface Tool {
    /** Stable identifier (never shown); used for preferences and logs. */
    String id();
    String title();
    String subtitle();
    Icon.Kind icon();
    /** The first screen of the tool; the home screen pushes it. */
    Screen open(Ctx c);
}
