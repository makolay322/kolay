package app.kolay.app.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.R;
import android.os.Build;

/**
 * Color tokens. Every screen reads colors from here; nothing hard-codes a hex value.
 * On Android 12+ the accent and neutral tones come from the system's wallpaper palette (the same source as the
 * launcher icon's colors), so the app follows the icon; older versions use the fixed teal set below.
 * {@link #softBg}/{@link #softFg} give a few close tonal variants of the accent so rows of icon tiles are not uniform.
 */
public final class Palette {
    public final boolean dark;
    public final int bg, surface, surface2, ink, muted, line, primary, onPrimary, primarySoft, onPrimarySoft, danger,
        ok, okSoft, warn, warnSoft, dangerSoft, track, thumb, scrim, ripple;
    private final int[] softBg, softFg;

    private Palette(Context c, boolean dark) {
        this.dark = dark;
        // Semantic colors stay fixed: red/green/amber must keep their meaning whatever the wallpaper is.
        if (dark) {
            danger = 0xFFFF8A80; ok = 0xFF6EE79A; okSoft = 0xFF173B25; warn = 0xFFFFC857; warnSoft = 0xFF3F3012; dangerSoft = 0xFF4A1F1C;
        } else {
            danger = 0xFFB3261E; ok = 0xFF1B7F3B; okSoft = 0xFFDDF3E4; warn = 0xFF9A5B00; warnSoft = 0xFFFFEBC8; dangerSoft = 0xFFFADAD7;
        }
        scrim = 0x99000000;
        if (Build.VERSION.SDK_INT >= 31) {
            Sys s = new Sys(c);
            if (dark) {
                bg = s.n1(900); surface = mix(s.n1(900), s.n1(800), 0.45f); surface2 = s.n1(800); ink = s.n1(100); muted = s.n2(300);
                line = mix(s.n1(800), s.n2(700), 0.5f); primary = s.a1(200); onPrimary = s.a1(800);
                primarySoft = s.a1(800); onPrimarySoft = s.a1(100); track = s.n2(700); thumb = s.n1(100);
                softBg = new int[]{primarySoft, s.a2(800), mix(s.a1(800), s.a1(700), 0.6f)};
                softFg = new int[]{onPrimarySoft, s.a2(100), s.a1(50)};
            } else {
                bg = s.n2(50); surface = s.n1(10); surface2 = s.n2(100); ink = s.n1(900); muted = s.n2(700);
                line = mix(s.n2(100), s.n2(200), 0.3f); primary = s.a1(600); onPrimary = s.a1(0);
                primarySoft = s.a1(100); onPrimarySoft = s.a1(800); track = s.n2(200); thumb = s.n1(10);
                softBg = new int[]{primarySoft, s.a2(100), mix(s.a1(100), s.a1(200), 0.6f)};
                softFg = new int[]{onPrimarySoft, s.a2(800), s.a1(900)};
            }
        } else if (dark) {
            bg = 0xFF0E1417; surface = 0xFF162025; surface2 = 0xFF1E2A30; ink = 0xFFE8EFF1; muted = 0xFF8FA1A9; line = 0xFF2A383F;
            primary = 0xFF2DD4BF; onPrimary = 0xFF042925; primarySoft = 0xFF133A37; onPrimarySoft = 0xFF7FF0E1;
            track = 0xFF34444C; thumb = 0xFFE8EFF1;
            softBg = new int[]{primarySoft, 0xFF1C3238, 0xFF0F4A45};
            softFg = new int[]{onPrimarySoft, 0xFFA9CFD8, 0xFFB2F5EC};
        } else {
            bg = 0xFFF4F6F7; surface = 0xFFFFFFFF; surface2 = 0xFFEAEFF1; ink = 0xFF14201F; muted = 0xFF64727C; line = 0xFFE0E6E9;
            primary = 0xFF0F766E; onPrimary = 0xFFFFFFFF; primarySoft = 0xFFD9F0EC; onPrimarySoft = 0xFF0B5A54;
            track = 0xFFC9D2D6; thumb = 0xFFFFFFFF;
            softBg = new int[]{primarySoft, 0xFFDCEBEF, 0xFFC3E8E2};
            softFg = new int[]{onPrimarySoft, 0xFF2C5560, 0xFF063F3B};
        }
        ripple = dark ? 0x22FFFFFF : (primary & 0x00FFFFFF) | 0x1E000000;
    }

    /** Background of the i-th tonal tile (icon boxes in lists); cycles through close variants of the accent. */
    public int softBg(int i) { return softBg[Math.floorMod(i, softBg.length)]; }
    /** Icon/text color matching {@link #softBg(int)}. */
    public int softFg(int i) { return softFg[Math.floorMod(i, softFg.length)]; }

    public static Palette of(Context c) {
        int night = c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return new Palette(c, night == Configuration.UI_MODE_NIGHT_YES);
    }

    /** Linear blend of two opaque colors, t = 0 → a, 1 → b. */
    static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /** The system's wallpaper-derived tonal palettes (Android 12+). Tone keys follow the platform names: 0 = white … 1000 = black. */
    @android.annotation.TargetApi(31)
    private static final class Sys {
        private final Context c;
        Sys(Context c) { this.c = c; }
        int a1(int t) { return get(t, A1); }
        int a2(int t) { return get(t, A2); }
        int n1(int t) { return get(t, N1); }
        int n2(int t) { return get(t, N2); }

        private static final int[] TONES = {0, 10, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};
        private static final int[] A1 = {R.color.system_accent1_0, R.color.system_accent1_10, R.color.system_accent1_50, R.color.system_accent1_100, R.color.system_accent1_200, R.color.system_accent1_300, R.color.system_accent1_400, R.color.system_accent1_500, R.color.system_accent1_600, R.color.system_accent1_700, R.color.system_accent1_800, R.color.system_accent1_900, R.color.system_accent1_1000};
        private static final int[] A2 = {R.color.system_accent2_0, R.color.system_accent2_10, R.color.system_accent2_50, R.color.system_accent2_100, R.color.system_accent2_200, R.color.system_accent2_300, R.color.system_accent2_400, R.color.system_accent2_500, R.color.system_accent2_600, R.color.system_accent2_700, R.color.system_accent2_800, R.color.system_accent2_900, R.color.system_accent2_1000};
        private static final int[] N1 = {R.color.system_neutral1_0, R.color.system_neutral1_10, R.color.system_neutral1_50, R.color.system_neutral1_100, R.color.system_neutral1_200, R.color.system_neutral1_300, R.color.system_neutral1_400, R.color.system_neutral1_500, R.color.system_neutral1_600, R.color.system_neutral1_700, R.color.system_neutral1_800, R.color.system_neutral1_900, R.color.system_neutral1_1000};
        private static final int[] N2 = {R.color.system_neutral2_0, R.color.system_neutral2_10, R.color.system_neutral2_50, R.color.system_neutral2_100, R.color.system_neutral2_200, R.color.system_neutral2_300, R.color.system_neutral2_400, R.color.system_neutral2_500, R.color.system_neutral2_600, R.color.system_neutral2_700, R.color.system_neutral2_800, R.color.system_neutral2_900, R.color.system_neutral2_1000};

        private int get(int tone, int[] ids) {
            for (int i = 0; i < TONES.length; i++) if (TONES[i] == tone) return c.getColor(ids[i]);
            throw new IllegalArgumentException("tone " + tone);
        }
    }
}
