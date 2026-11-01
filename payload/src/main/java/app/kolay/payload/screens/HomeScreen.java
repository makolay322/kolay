package app.kolay.payload.screens;

import android.view.*;
import android.widget.*;
import app.kolay.payload.tools.Tool;
import app.kolay.payload.tools.Tools;
import app.kolay.payload.ui.*;
import java.util.ArrayList;
import java.util.List;

/** Landing page: the toolbox. */
public final class HomeScreen extends Screen {
    private final List<View> entrance = new ArrayList<>();
    private LinearLayout banner;
    private Logo logo;

    @Override public View create() {
        Palette p = c.p;
        LinearLayout col = new LinearLayout(c.a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(c.dp(Ui.GUTTER), c.dp(18), c.dp(Ui.GUTTER), c.dp(24));

        // Header: wordmark + settings
        LinearLayout head = new LinearLayout(c.a);
        head.setGravity(Gravity.CENTER_VERTICAL);
        logo = new Logo(c.a, p.dark);
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(c.dp(62), c.dp(46));
        ll.setMarginEnd(c.dp(14));
        head.addView(logo, ll);
        LinearLayout titles = new LinearLayout(c.a);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView name = Ui.text(c, "Kolay", 36, p.ink, 700);
        name.setLetterSpacing(-0.02f);
        titles.addView(name);
        TextView sub = Ui.text(c, "Araç kutun", 15, p.muted, 400);
        titles.addView(sub, Ui.lp(-2, -2, 0, c.dp(4), 0, 0));
        head.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(Ui.iconButton(c, Icon.Kind.SETTINGS, "Ayarlar", () -> c.nav.push(new SettingsScreen())));
        col.addView(head);
        entrance.add(head);

        // "New version" banner (only when a staged payload waits)
        banner = new LinearLayout(c.a);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPadding(c.dp(16), c.dp(12), c.dp(16), c.dp(12));
        Ui.surface(c, banner, p.primarySoft, 16, false);
        banner.setClickable(true);
        banner.addView(Ui.icon(c, Icon.Kind.REFRESH, p.onPrimarySoft, 20));
        TextView bt = Ui.text(c, "Yeni sürüm hazır — dokun, yenilensin", 14, p.onPrimarySoft, 600);
        banner.addView(bt, Ui.lp(0, -2, c.dp(12), 0, 0, 0));
        ((LinearLayout.LayoutParams) bt.getLayoutParams()).weight = 1;
        banner.setOnClickListener(v -> { Ui.haptic(v, HapticFeedbackConstants.CONFIRM); c.host.restart(); });
        Ui.press(banner);
        banner.setVisibility(View.GONE);
        col.addView(banner, Ui.lp(-1, -2, 0, c.dp(18), 0, 0));

        TextView label = Ui.label(c, "ARAÇLAR");
        col.addView(label, Ui.lp(-2, -2, c.dp(4), c.dp(26), 0, c.dp(12)));
        entrance.add(label);

        // Two-column tile grid for everyday tools; the rarely used ones follow as slim full-width rows.
        final int gap = c.dp(12);
        java.util.List<Tool> tiles = new java.util.ArrayList<>(), slim = new java.util.ArrayList<>();
        for (Tool t : Tools.ALL) (t.compact() ? slim : tiles).add(t);
        for (int i = 0; i < tiles.size(); i += 2) {
            LinearLayout row = new LinearLayout(c.a);
            for (int j = i; j < i + 2; j++) {
                View tile = j < tiles.size() ? toolCard(tiles.get(j)) : new Space(c.a);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
                if (j == i) lp.setMarginEnd(gap / 2); else lp.setMarginStart(gap / 2);
                row.addView(tile, lp);
                if (j < tiles.size()) entrance.add(tile);
            }
            col.addView(row, Ui.lp(-1, -2, 0, 0, 0, gap));
        }
        for (Tool t : slim) {
            View row = slimRow(t);
            col.addView(row, Ui.lp(-1, c.dp(68), 0, 0, 0, gap));
            entrance.add(row);
        }

        TextView foot = Ui.text(c, "Modül " + c.host.payloadName() + " · Kabuk " + app.kolay.payload.Versions.shellName(c.a, c.host.shellVersion()), 12, p.muted, 400);
        foot.setGravity(Gravity.CENTER);
        col.addView(foot, Ui.lp(-1, -2, 0, c.dp(16), 0, 0));
        entrance.add(foot);

        for (View v : entrance) v.setAlpha(0f);
        ScrollView sv = new ScrollView(c.a);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(col);
        return sv;
    }

    private View toolCard(Tool t) {
        Palette p = c.p;
        LinearLayout card = new LinearLayout(c.a);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(c.dp(16), c.dp(16), c.dp(16), c.dp(16));
        card.setMinimumHeight(c.dp(148));
        Ui.surface(c, card, p.surface, 22, true);
        card.setClickable(true);

        FrameLayout tile = new FrameLayout(c.a);
        tile.setBackground(Ui.rounded(c, p.primarySoft, 16));
        tile.addView(Ui.icon(c, t.icon(), p.onPrimarySoft, 26), new FrameLayout.LayoutParams(c.dp(26), c.dp(26), Gravity.CENTER));
        card.addView(tile, new LinearLayout.LayoutParams(c.dp(52), c.dp(52)));

        TextView title = Ui.text(c, t.title(), 16, p.ink, 600);
        title.setSingleLine(true); title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        card.addView(title, Ui.lp(-1, -2, 0, c.dp(14), 0, 0));
        TextView sub = Ui.text(c, t.subtitle(), 13, p.muted, 400);
        sub.setMaxLines(2); sub.setEllipsize(android.text.TextUtils.TruncateAt.END); sub.setLineSpacing(0, 1.1f);
        card.addView(sub, Ui.lp(-1, -2, 0, c.dp(4), 0, 0));
        card.setContentDescription(t.title() + ". " + t.subtitle());

        card.setOnClickListener(v -> { Ui.haptic(v, HapticFeedbackConstants.VIRTUAL_KEY); c.nav.push(t.open(c)); });
        Ui.press(card);
        return card;
    }

    /** A slim wide row: small icon, title and subtitle on one line. */
    private View slimRow(Tool t) {
        Palette p = c.p;
        LinearLayout row = new LinearLayout(c.a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(c.dp(14), 0, c.dp(16), 0);
        Ui.surface(c, row, p.surface, 22, true);
        row.setClickable(true);
        FrameLayout badge = new FrameLayout(c.a);
        badge.setBackground(Ui.rounded(c, p.primarySoft, 12));
        badge.addView(Ui.icon(c, t.icon(), p.onPrimarySoft, 20), new FrameLayout.LayoutParams(c.dp(20), c.dp(20), Gravity.CENTER));
        row.addView(badge, new LinearLayout.LayoutParams(c.dp(40), c.dp(40)));
        LinearLayout txt = new LinearLayout(c.a);
        txt.setOrientation(LinearLayout.VERTICAL);
        TextView title = Ui.text(c, t.title(), 15, p.ink, 600);
        title.setSingleLine(true); title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        txt.addView(title);
        TextView sub = Ui.text(c, t.subtitle(), 13, p.muted, 400);
        sub.setSingleLine(true); sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        txt.addView(sub, Ui.lp(-1, -2, 0, c.dp(3), 0, 0));
        row.setContentDescription(t.title() + ". " + t.subtitle());
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, -2, 1);
        tl.setMarginStart(c.dp(12));
        row.addView(txt, tl);
        row.addView(Ui.icon(c, Icon.Kind.CHEVRON, p.muted, 18), new LinearLayout.LayoutParams(c.dp(18), c.dp(18)));
        row.setOnClickListener(v -> { Ui.haptic(v, HapticFeedbackConstants.VIRTUAL_KEY); c.nav.push(t.open(c)); });
        Ui.press(row);
        return row;
    }

    @Override public void onShown() {
        long d = 40;
        for (View v : entrance) { Ui.appear(v, d); d += 55; }
        logo.intro();
        c.host.onUpdateReady(this::showBanner);
        if (c.host.updateReady()) showBanner();
    }

    private boolean wentAway;

    @Override public void onPause() { wentAway = true; }

    /** Back from the background (or from another app): replay the logo entrance. */
    @Override public void onResume() {
        if (!wentAway) return;
        wentAway = false;
        logo.intro();
    }

    private void showBanner() {
        if (banner == null || banner.getVisibility() == View.VISIBLE) return;
        banner.setVisibility(View.VISIBLE);
        Ui.appear(banner, 0);
    }
}
