package app.kolay.app.tools.tibbi;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import app.kolay.app.ui.*;

/** One calculator: inputs scroll, the result stays pinned at the bottom and updates on every change. Values live only in memory. */
final class CalcScreen extends Screen {
    private final Calc k;
    private Calc.V v;
    private TextView value, unit, verdict, detail;
    private LinearLayout resultCard;

    CalcScreen(Calc k) { this.k = k; }

    @Override public View create() {
        Palette p = c.p;
        v = k.blank();
        LinearLayout page = new LinearLayout(c.a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.addView(new Header(c, k.title, k.sub, () -> c.nav.pop()).view);

        LinearLayout col = new LinearLayout(c.a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(c.dp(Ui.GUTTER), 0, c.dp(Ui.GUTTER), c.dp(Ui.GUTTER));

        LinearLayout nums = null, checks = null;
        for (Calc.Field f : k.fields) {
            switch (f.kind) {
                case NUM:
                    if (nums == null) { nums = Ui.card(c, p.surface, 16); col.addView(nums, Ui.lp(-1, -2, 0, 0, 0, c.dp(Ui.GAP))); checks = null; }
                    nums.addView(numRow(f), Ui.lp(-1, -2, 0, nums.getChildCount() == 0 ? 0 : c.dp(12), 0, 0));
                    break;
                case CHOICE:
                    if (nums == null) { nums = Ui.card(c, p.surface, 16); col.addView(nums, Ui.lp(-1, -2, 0, 0, 0, c.dp(Ui.GAP))); checks = null; }
                    nums.addView(choiceRow(f), Ui.lp(-1, -2, 0, nums.getChildCount() == 0 ? 0 : c.dp(14), 0, 0));
                    break;
                case CHECK:
                    if (checks == null) { checks = Ui.card(c, p.surface, 6); col.addView(checks, Ui.lp(-1, -2, 0, 0, 0, c.dp(Ui.GAP))); nums = null; }
                    checks.addView(checkRow(f));
                    break;
            }
        }
        if (k.note != null) {
            TextView note = Ui.text(c, k.note, 12, p.muted, 400);
            note.setLineSpacing(0, 1.25f);
            col.addView(note, Ui.lp(-1, -2, c.dp(4), c.dp(2), c.dp(4), 0));
        }
        ScrollView sv = new ScrollView(c.a);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(col);
        page.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));

        // pinned result
        LinearLayout bar = Ui.bottomBar(c);
        resultCard = new LinearLayout(c.a);
        resultCard.setOrientation(LinearLayout.VERTICAL);
        resultCard.setPadding(c.dp(18), c.dp(14), c.dp(18), c.dp(14));
        LinearLayout top = new LinearLayout(c.a);
        top.setGravity(Gravity.BOTTOM);
        value = Ui.text(c, "—", 30, p.ink, 700);
        top.addView(value);
        unit = Ui.text(c, "", 14, p.muted, 500);
        top.addView(unit, Ui.lp(-2, -2, c.dp(6), 0, 0, c.dp(4)));
        resultCard.addView(top);
        verdict = Ui.text(c, "", 15, p.ink, 600);
        resultCard.addView(verdict, Ui.lp(-2, -2, 0, c.dp(6), 0, 0));
        detail = Ui.text(c, "", 12, p.muted, 400);
        detail.setVisibility(View.GONE);
        resultCard.addView(detail, Ui.lp(-2, -2, 0, c.dp(4), 0, 0));
        bar.addView(resultCard, Ui.lp(-1, -2));
        page.addView(bar);
        compute();
        return page;
    }

    private View numRow(Calc.Field f) {
        Palette p = c.p;
        LinearLayout r = new LinearLayout(c.a);
        r.setGravity(Gravity.CENTER_VERTICAL);
        TextView l = Ui.text(c, f.label, 15, p.ink, 500);
        r.addView(l, new LinearLayout.LayoutParams(0, -2, 1));
        EditText e = Ui.input(c, f.hint == null ? "" : f.hint, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setKeyListener(DigitsKeyListener.getInstance("0123456789.,"));  // decimal comma as well as point
        e.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        e.setPadding(c.dp(12), c.dp(11), c.dp(12), c.dp(11));
        e.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        e.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) {}
            @Override public void afterTextChanged(Editable s) { v.m.put(f.key, Calc.parse(s.toString())); compute(); }
        });
        r.addView(e, new LinearLayout.LayoutParams(c.dp(120), -2));
        TextView u = Ui.text(c, f.unit == null ? "" : f.unit, 13, p.muted, 400);
        LinearLayout.LayoutParams ul = new LinearLayout.LayoutParams(c.dp(f.unit == null ? 0 : 52), -2);
        ul.setMarginStart(c.dp(f.unit == null ? 0 : 8));
        r.addView(u, ul);
        return r;
    }

    private View choiceRow(Calc.Field f) {
        LinearLayout r = new LinearLayout(c.a);
        r.setOrientation(LinearLayout.VERTICAL);
        r.addView(Ui.text(c, f.label, 15, c.p.ink, 500));
        FrameLayout holder = new FrameLayout(c.a);
        r.addView(holder, Ui.lp(-1, -2, 0, c.dp(8), 0, 0));
        paintChoice(holder, f, f.initial);
        return r;
    }

    /** Up to three options: a segmented control. More (Glasgow): a vertical list of selectable rows. */
    private void paintChoice(FrameLayout holder, Calc.Field f, int sel) {
        holder.removeAllViews();
        if (f.options.length <= 3) {
            holder.addView(Ui.segmented(c, f.options, null, sel, i -> pick(holder, f, i)), new FrameLayout.LayoutParams(-1, c.dp(44)));
            return;
        }
        LinearLayout list = new LinearLayout(c.a);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackground(Ui.rounded(c, c.p.surface2, 14));
        list.setPadding(c.dp(4), c.dp(4), c.dp(4), c.dp(4));
        for (int i = 0; i < f.options.length; i++) {
            boolean on = i == sel;
            TextView t = Ui.text(c, f.options[i], 14, on ? c.p.onPrimary : c.p.ink, on ? 700 : 400);
            t.setPadding(c.dp(12), c.dp(11), c.dp(12), c.dp(11));
            if (on) t.setBackground(Ui.rounded(c, c.p.primary, 10));
            else {
                final int idx = i;
                t.setClickable(true);
                t.setOnClickListener(x -> { Ui.haptic(x, HapticFeedbackConstants.CLOCK_TICK); pick(holder, f, idx); });
            }
            t.setContentDescription(f.options[i] + (on ? ", seçili" : ""));
            list.addView(t, Ui.lp(-1, -2));
        }
        holder.addView(list, new FrameLayout.LayoutParams(-1, -2));
    }

    private void pick(FrameLayout holder, Calc.Field f, int i) {
        v.m.put(f.key, f.values[i]);
        paintChoice(holder, f, i);
        compute();
    }

    private View checkRow(Calc.Field f) {
        Palette p = c.p;
        LinearLayout r = new LinearLayout(c.a);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(c.dp(12), c.dp(10), c.dp(10), c.dp(10));
        r.setBackground(Ui.rounded(c, 0, 14));
        TextView l = Ui.text(c, f.label, 14, p.ink, 400);
        l.setLineSpacing(0, 1.15f);
        r.addView(l, new LinearLayout.LayoutParams(0, -2, 1));
        TextView pts = Ui.text(c, "+" + Calc.score(f.points), 13, p.muted, 600);
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(-2, -2);
        pl.setMarginStart(c.dp(8)); pl.setMarginEnd(c.dp(10));
        r.addView(pts, pl);
        Toggle t = new Toggle(c.a, p.track, p.primary, p.thumb, false);
        t.onChange(on -> { v.m.put(f.key, on ? f.points : 0); compute(); });
        r.addView(t);
        r.setClickable(true);
        r.setOnClickListener(x -> t.performClick());
        return r;
    }

    private void compute() {
        if (value == null) return;
        Palette p = c.p;
        Calc.Result res = k.fn.run(v);
        int bg = p.surface2, fg = p.ink;
        if (res == null) {
            value.setText("—"); unit.setText(""); verdict.setText("Değerleri gir"); verdict.setTextColor(p.muted);
            detail.setVisibility(View.GONE);
        } else if (res.error != null) {
            value.setText("—"); unit.setText(""); verdict.setText(res.error); verdict.setTextColor(p.danger);
            detail.setVisibility(View.GONE);
        } else {
            value.setText(res.value); unit.setText(res.unit == null ? "" : res.unit);
            verdict.setText(res.verdict);
            switch (res.level) {
                case 1: bg = p.okSoft; fg = p.ok; break;
                case 2: bg = p.warnSoft; fg = p.warn; break;
                case 3: bg = p.dangerSoft; fg = p.danger; break;
            }
            verdict.setTextColor(fg);
            detail.setText(res.detail == null ? "" : res.detail);
            detail.setVisibility(res.detail == null ? View.GONE : View.VISIBLE);
        }
        value.setTextColor(res != null && res.error == null && res.level > 0 ? fg : p.ink);
        resultCard.setBackground(Ui.rounded(c, bg, 20));
    }
}
