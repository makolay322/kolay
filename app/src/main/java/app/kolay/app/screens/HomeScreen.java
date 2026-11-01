package app.kolay.app.screens;

import android.view.*;
import android.widget.*;
import app.kolay.app.tools.Tool;
import app.kolay.app.tools.Tools;
import app.kolay.app.ui.*;
import java.util.ArrayList;
import java.util.List;

/** Landing page: the toolbox. */
public final class HomeScreen extends Screen {
    private final List<View> entrance = new ArrayList<>();
    private LinearLayout banner;
    private TextView bannerText;
    private Logo logo;
    private LinearLayout col;
    private TextView name, sub, toolsLabel;
    private boolean compact;

    @Override public View create() {
        Palette p = c.p;
        col = new LinearLayout(c.a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(c.dp(Ui.GUTTER), c.dp(HEAD_PAD_FULL), c.dp(Ui.GUTTER), c.dp(24));

        // Header: wordmark + settings. Starts large for the logo's entrance, then settles into a slim bar (see collapse()).
        LinearLayout head = new LinearLayout(c.a);
        head.setGravity(Gravity.CENTER_VERTICAL);
        logo = new Logo(c.a, p.dark);
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(c.dp(LOGO_W_FULL), c.dp(LOGO_H_FULL));   // width includes the O's overshoot room
        ll.setMarginEnd(c.dp(12));
        head.addView(logo, ll);
        LinearLayout titles = new LinearLayout(c.a);
        titles.setOrientation(LinearLayout.VERTICAL);
        name = Ui.text(c, "Kolay", NAME_SP_FULL, p.ink, 700);
        name.setLetterSpacing(-0.02f);
        titles.addView(name);
        sub = Ui.text(c, "Araç kutun", 15, p.muted, 400);
        titles.addView(sub, Ui.lp(-2, -2, 0, c.dp(4), 0, 0));
        head.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(Ui.iconButton(c, Icon.Kind.SETTINGS, "Ayarlar", () -> c.nav.push(new SettingsScreen())));
        col.addView(head);
        entrance.add(head);

        // "New version" banner (only when a downloaded update waits)
        banner = new LinearLayout(c.a);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPadding(c.dp(14), c.dp(8), c.dp(14), c.dp(8));
        Ui.surface(c, banner, p.primarySoft, 16, false);
        banner.setClickable(true);
        banner.addView(Ui.icon(c, Icon.Kind.REFRESH, p.onPrimarySoft, 18));
        bannerText = Ui.text(c, "Yeni sürüm hazır — dokun, kurulsun", 13, p.onPrimarySoft, 600);
        TextView bt = bannerText;
        banner.addView(bt, Ui.lp(0, -2, c.dp(12), 0, 0, 0));
        ((LinearLayout.LayoutParams) bt.getLayoutParams()).weight = 1;
        banner.setOnClickListener(v -> {
            Ui.haptic(v, HapticFeedbackConstants.CONFIRM);
            c.host.installUpdate(msg -> bannerText.setText(msg));
        });
        Ui.press(banner);
        banner.setVisibility(View.GONE);
        col.addView(banner, Ui.lp(-1, -2, 0, c.dp(12), 0, 0));

        TextView label = Ui.label(c, "ARAÇLAR");
        toolsLabel = label;
        col.addView(label, Ui.lp(-2, -2, c.dp(4), c.dp(LABEL_TOP_FULL), 0, c.dp(12)));
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

        TextView foot = Ui.text(c, "Sürüm " + c.host.versionName(), 12, p.muted, 400);
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
        if (!compact) c.ui.postDelayed(this::collapse, Logo.INTRO_MS + 150);
        c.host.onUpdateReady(this::showBanner);
        if (c.host.updateReady()) showBanner();
    }

    // Header sizes (dp / sp): large while the logo enters, slim afterwards.
    private static final int LOGO_W_FULL = 64, LOGO_H_FULL = 46, LOGO_W_SLIM = 44, LOGO_H_SLIM = 32;
    private static final int NAME_SP_FULL = 36, NAME_SP_SLIM = 26, HEAD_PAD_FULL = 18, HEAD_PAD_SLIM = 8, LABEL_TOP_FULL = 26, LABEL_TOP_SLIM = 16;

    /** After the entrance: the header shrinks (smaller logo and wordmark, subtitle folds away) so the tools move up. */
    private void collapse() {
        if (compact || name == null || !name.isAttachedToWindow()) return;
        compact = true;
        LinearLayout.LayoutParams ll = (LinearLayout.LayoutParams) logo.getLayoutParams();
        int subH = sub.getHeight();
        LinearLayout.LayoutParams sl = (LinearLayout.LayoutParams) sub.getLayoutParams();
        int subTop = sl.topMargin;
        LinearLayout.LayoutParams tl = (LinearLayout.LayoutParams) toolsLabel.getLayoutParams();
        android.animation.ValueAnimator a = android.animation.ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(480);
        a.setInterpolator(Ui.EMPH);
        a.addUpdateListener(v -> {
            float t = (float) v.getAnimatedValue();
            ll.width = c.dp(LOGO_W_FULL + (LOGO_W_SLIM - LOGO_W_FULL) * t);
            ll.height = c.dp(LOGO_H_FULL + (LOGO_H_SLIM - LOGO_H_FULL) * t);
            logo.setLayoutParams(ll);
            name.setTextSize(NAME_SP_FULL + (NAME_SP_SLIM - NAME_SP_FULL) * t);
            sub.setAlpha(1f - Math.min(1f, t * 1.6f));
            sl.height = Math.round(subH * (1f - t));
            sl.topMargin = Math.round(subTop * (1f - t));
            sub.setLayoutParams(sl);
            tl.topMargin = c.dp(LABEL_TOP_FULL + (LABEL_TOP_SLIM - LABEL_TOP_FULL) * t);
            toolsLabel.setLayoutParams(tl);
            col.setPadding(col.getPaddingLeft(), c.dp(HEAD_PAD_FULL + (HEAD_PAD_SLIM - HEAD_PAD_FULL) * t), col.getPaddingRight(), col.getPaddingBottom());
        });
        a.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator x) { sub.setVisibility(View.GONE); }
        });
        a.start();
    }

    private boolean wentAway;

    @Override public void onPause() { wentAway = true; }

    /** Back from the background (or from another app): replay the logo entrance. */
    @Override public void onResume() {
        if (c.host.updateReady()) showBanner();
        if (!wentAway) return;
        wentAway = false;
        logo.intro();
    }

    private void showBanner() {
        if (banner == null) return;
        String n = c.host.updateName();
        bannerText.setText("Yeni sürüm hazır" + (n.isEmpty() ? "" : " (" + n + ")") + " — dokun, kurulsun");
        if (banner.getVisibility() == View.VISIBLE) return;
        banner.setVisibility(View.VISIBLE);
        Ui.appear(banner, 0);
    }
}
