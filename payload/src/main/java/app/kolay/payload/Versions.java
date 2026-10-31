package app.kolay.payload;

import android.content.Context;

/** Human-readable version names (the host API only exposes version codes). */
public final class Versions {
    private Versions() {}

    public static String shellName(Context ctx, int fallbackCode) {
        try {
            String n = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
            if (n != null && !n.isEmpty()) return n;
        } catch (Exception ignored) { }
        return String.valueOf(fallbackCode);
    }
}
