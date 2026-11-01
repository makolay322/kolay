package app.kolay.app.ui;

import android.view.View;

/** One navigable page. Created by {@link Nav}; build the view in {@link #create()} using {@link #c}. */
public abstract class Screen {
    protected Ctx c;
    View view;

    final void attach(Ctx c) { this.c = c; }
    public abstract View create();
    /** After the enter transition finished (first time only). Start entrance animations here. */
    public void onShown() {}
    /** Activity resumed / paused while this screen is on top (camera and other exclusive resources belong here). */
    public void onResume() {}
    public void onPause() {}
    public void onPermission(int req, String[] perms, int[] results) {}
    /** @return true if consumed (e.g. cancelled something) instead of leaving the screen. */
    public boolean onBack() { return false; }
    public void onDestroy() {}
}
