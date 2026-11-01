package app.kolay.payload.tools.halal;

import app.kolay.payload.tools.Tool;
import app.kolay.payload.ui.Ctx;
import app.kolay.payload.ui.Icon;
import app.kolay.payload.ui.Screen;

/** Barcode (or ingredient-label photo) -> Hanafi halal / haram / doubtful assessment. */
public final class HelalTool implements Tool {
    @Override public String id() { return "helal"; }
    @Override public String title() { return "Helal Sorgu"; }
    @Override public String subtitle() { return "Barkod · Hanefi mezhebi"; }
    @Override public Icon.Kind icon() { return Icon.Kind.BARCODE; }
    @Override public Screen open(Ctx c) { return new ScanScreen(null); }
}
