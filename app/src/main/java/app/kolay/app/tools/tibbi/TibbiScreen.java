package app.kolay.app.tools.tibbi;

import android.view.*;
import android.widget.*;
import app.kolay.app.ui.*;
import java.util.ArrayList;
import java.util.List;

/** Calculator list, grouped. */
final class TibbiScreen extends Screen {
    private final List<View> entrance = new ArrayList<>();

    @Override public View create() {
        Palette p = c.p;
        LinearLayout page = new LinearLayout(c.a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.addView(new Header(c, "Tıbbi Hesap", "Çevrimdışı · girilen değerler kaydedilmez", () -> c.nav.pop()).view);

        LinearLayout col = new LinearLayout(c.a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(c.dp(Ui.GUTTER), 0, c.dp(Ui.GUTTER), c.dp(Ui.GUTTER));
        String group = null;
        for (Calc k : Calcs.ALL) {
            if (!k.group.equals(group)) {
                group = k.group;
                TextView l = Ui.label(c, group);
                col.addView(l, Ui.lp(-2, -2, c.dp(4), c.dp(group.equals(Calcs.ALL[0].group) ? 4 : 18), 0, c.dp(8)));
                entrance.add(l);
            }
            View r = row(k);
            col.addView(r, Ui.lp(-1, -2, 0, 0, 0, c.dp(8)));
            entrance.add(r);
        }
        TextView note = Ui.text(c, "Karar desteği içindir; klinik değerlendirmenin yerini tutmaz.", 12, p.muted, 400);
        note.setGravity(Gravity.CENTER);
        col.addView(note, Ui.lp(-1, -2, 0, c.dp(10), 0, 0));
        entrance.add(note);

        ScrollView sv = new ScrollView(c.a);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(col);
        page.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        for (View v : entrance) v.setAlpha(0f);
        return page;
    }

    private View row(Calc k) {
        Palette p = c.p;
        LinearLayout r = new LinearLayout(c.a);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(c.dp(16), c.dp(12), c.dp(12), c.dp(12));
        Ui.surface(c, r, p.surface, 18, true);
        LinearLayout t = new LinearLayout(c.a);
        t.setOrientation(LinearLayout.VERTICAL);
        TextView title = Ui.text(c, k.title, 15, p.ink, 600);
        t.addView(title);
        TextView sub = Ui.text(c, k.sub, 12, p.muted, 400);
        t.addView(sub, Ui.lp(-2, -2, 0, c.dp(3), 0, 0));
        r.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(Ui.icon(c, Icon.Kind.CHEVRON, p.muted, 18));
        r.setClickable(true);
        r.setOnClickListener(v -> { Ui.haptic(v, HapticFeedbackConstants.VIRTUAL_KEY); c.nav.push(new CalcScreen(k)); });
        Ui.press(r);
        return r;
    }

    @Override public void onShown() { for (int i = 0; i < entrance.size(); i++) Ui.appear(entrance.get(i), Math.min(i, 10) * 25L); }
}
