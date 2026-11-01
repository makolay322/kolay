package app.kolay.app.ui;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import app.kolay.shell.Host;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Everything a screen needs: host services, theme, navigation, a UI handler and a background pool. */
public final class Ctx {
    public final Host host;
    public final Activity a;
    public final Palette p;
    public final Handler ui = new Handler(Looper.getMainLooper());
    public final ExecutorService io = Executors.newFixedThreadPool(3);
    public Nav nav;

    public Ctx(Host host, Palette p) { this.host = host; this.a = host.activity(); this.p = p; }

    public int dp(float v) { return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, a.getResources().getDisplayMetrics())); }
}
