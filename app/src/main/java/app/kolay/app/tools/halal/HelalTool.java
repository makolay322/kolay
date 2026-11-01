package app.kolay.app.tools.halal;

import app.kolay.app.tools.Tool;
import app.kolay.app.ui.Ctx;
import app.kolay.app.ui.Icon;
import app.kolay.app.ui.Screen;

/** Barcode (or ingredient-label photo) -> Hanafi halal / haram / doubtful assessment. */
public final class HelalTool implements Tool {
    @Override public String id() { return "helal"; }
    @Override public String title() { return "Helal Sorgu"; }
    @Override public String subtitle() { return "Barkod veya içindekiler"; }
    @Override public Icon.Kind icon() { return Icon.Kind.BARCODE; }
    @Override public Screen open(Ctx c) { return new ScanScreen(null); }
}
