package app.kolay.payload;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import app.kolay.payload.screens.HomeScreen;
import app.kolay.payload.ui.*;
import app.kolay.sdk.Host;
import app.kolay.sdk.Module;

/** Payload entry point: sets up theme, navigation and the home screen inside the shell's activity. */
public class Entry implements Module {
    private Ctx c;

    @Override public void onCreate(Host host, Bundle saved) {
        Palette p = Palette.of(host.activity());
        c = new Ctx(host, p);
        android.app.Activity a = c.a;

        a.getWindow().setStatusBarColor(p.bg);
        a.getWindow().setNavigationBarColor(p.bg);
        int flags = View.SYSTEM_UI_FLAG_VISIBLE;
        if (!p.dark) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        a.getWindow().getDecorView().setSystemUiVisibility(flags);

        FrameLayout root = new FrameLayout(a);
        root.setBackgroundColor(p.bg);
        root.setFitsSystemWindows(true);
        a.setContentView(root);

        new Nav(c, root).push(new HomeScreen());
        host.ready();
    }

    @Override public void onPermissionResult(int req, String[] perms, int[] results) {
        Screen s = c.nav.top();
        if (s != null) s.onPermission(req, perms, results);
    }

    @Override public boolean onBackPressed() {
        Screen s = c.nav.top();
        if (s != null && s.onBack()) return true;
        return c.nav.pop();
    }

    @Override public void onDestroy() {
        c.host.onUpdateReady(null);
        c.nav.destroyAll();
        c.io.shutdownNow();
    }
}
