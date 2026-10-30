package app.kolay.payload.ui;

import android.content.Context;
import android.content.res.Configuration;

/** Color tokens. Every screen reads colors from here; nothing hard-codes a hex value. */
public final class Palette {
    public final boolean dark;
    public final int bg, surface, surface2, ink, muted, line, primary, onPrimary, primarySoft, onPrimarySoft, danger;

    private Palette(boolean dark) {
        this.dark = dark;
        if (dark) {
            bg = 0xFF0E1417; surface = 0xFF162025; surface2 = 0xFF1E2A30; ink = 0xFFE8EFF1; muted = 0xFF8FA1A9; line = 0xFF2A383F;
            primary = 0xFF2DD4BF; onPrimary = 0xFF042925; primarySoft = 0xFF133A37; onPrimarySoft = 0xFF7FF0E1; danger = 0xFFFF8A80;
        } else {
            bg = 0xFFF4F6F7; surface = 0xFFFFFFFF; surface2 = 0xFFEAEFF1; ink = 0xFF14201F; muted = 0xFF64727C; line = 0xFFE0E6E9;
            primary = 0xFF0F766E; onPrimary = 0xFFFFFFFF; primarySoft = 0xFFD9F0EC; onPrimarySoft = 0xFF0B5A54; danger = 0xFFB3261E;
        }
    }

    public static Palette of(Context c) {
        int night = c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return new Palette(night == Configuration.UI_MODE_NIGHT_YES);
    }
}
