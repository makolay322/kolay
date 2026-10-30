package app.kolay.payload.ui;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared page header: back button, title and a (swappable) subtitle. */
public final class Header {
    public final LinearLayout view;
    public final TextView subtitle;

    public Header(Ctx c, String title, String sub, Runnable onBack) { this(c, title, sub, onBack, null); }

    public Header(Ctx c, String title, String sub, Runnable onBack, View trailing) {
        view = new LinearLayout(c.a);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(c.dp(16), c.dp(10), c.dp(16), c.dp(10));
        view.addView(Ui.iconButton(c, Icon.Kind.BACK, "Geri", onBack));
        LinearLayout t = new LinearLayout(c.a);
        t.setOrientation(LinearLayout.VERTICAL);
        t.addView(Ui.text(c, title, 22, c.p.ink, 700));
        subtitle = Ui.text(c, sub, 13, c.p.muted, 400);
        t.addView(subtitle, Ui.lp(-2, -2, 0, c.dp(2), 0, 0));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.setMarginStart(c.dp(14));
        view.addView(t, lp);
        if (trailing != null) view.addView(trailing);
    }
}
