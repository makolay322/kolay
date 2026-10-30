package app.kolay.payload.ui;

import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.view.animation.*;
import android.widget.*;

/** Small view factory + motion helpers. Programmatic UI: no XML resources inside a payload. */
public final class Ui {
    private Ui() {}

    // Material-style easing curves.
    public static final Interpolator EMPH = new PathInterpolator(0.05f, 0.7f, 0.1f, 1f);   // emphasized decelerate: entering
    public static final Interpolator STD = new PathInterpolator(0.2f, 0f, 0f, 1f);         // standard
    public static final Interpolator EXIT = new PathInterpolator(0.3f, 0f, 0.8f, 0.15f);   // leaving
    public static final Interpolator POP = new OvershootInterpolator(2.4f);

    public enum Style { PRIMARY, TONAL, GHOST }

    public static TextView text(Ctx c, CharSequence s, float sp, int color, int weight) {
        TextView t = new TextView(c.a);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        t.setTypeface(Typeface.create(Typeface.DEFAULT, weight, false));
        t.setIncludeFontPadding(false);
        return t;
    }

    public static GradientDrawable rounded(Ctx c, int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(c.dp(radiusDp)); return d;
    }

    public static GradientDrawable outlined(Ctx c, int fill, int stroke, float radiusDp) {
        GradientDrawable d = rounded(c, fill, radiusDp); d.setStroke(Math.max(1, c.dp(1)), stroke); return d;
    }

    /** Rounded background with touch ripple and a matching shadow outline. */
    public static void surface(Ctx c, View v, int fill, float radiusDp, boolean raised) {
        Drawable content = c.p.dark ? outlined(c, fill, c.p.line, radiusDp) : rounded(c, fill, radiusDp);
        GradientDrawable mask = rounded(c, Color.WHITE, radiusDp);
        int rip = c.p.dark ? 0x22FFFFFF : 0x180F766E;
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(rip), content, mask));
        final float r = c.dp(radiusDp);
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline o) { o.setRoundRect(0, 0, view.getWidth(), view.getHeight(), r); }
        });
        if (raised && !c.p.dark) v.setElevation(c.dp(2));
    }

    /** Subtle scale-down while the finger is on the view. Does not consume the touch. */
    public static void press(View v) {
        v.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).setInterpolator(STD).start(); break;
                case MotionEvent.ACTION_UP: case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(POP).start(); break;
            }
            return false;
        });
    }

    public static void haptic(View v, int kind) { v.performHapticFeedback(kind); }

    public static ImageView icon(Ctx c, Icon.Kind k, int color, float sizeDp) {
        ImageView iv = new ImageView(c.a);
        iv.setImageDrawable(new Icon(k, color));
        iv.setLayoutParams(new ViewGroup.LayoutParams(c.dp(sizeDp), c.dp(sizeDp)));
        return iv;
    }

    /** Round tappable icon button (back, settings…). */
    public static FrameLayout iconButton(Ctx c, Icon.Kind k, String desc, Runnable onClick) {
        FrameLayout f = new FrameLayout(c.a);
        surface(c, f, c.p.surface2, 22, false);
        ImageView iv = icon(c, k, c.p.ink, 22);
        f.addView(iv, new FrameLayout.LayoutParams(c.dp(22), c.dp(22), Gravity.CENTER));
        f.setContentDescription(desc); f.setClickable(true);
        f.setOnClickListener(v -> { haptic(v, HapticFeedbackConstants.VIRTUAL_KEY); onClick.run(); });
        press(f);
        f.setLayoutParams(new LinearLayout.LayoutParams(c.dp(44), c.dp(44)));
        return f;
    }

    public static TextView button(Ctx c, String label, Style st, Icon.Kind k) {
        TextView b = text(c, label, 16, 0, 600);
        b.setGravity(Gravity.CENTER); b.setClickable(true); b.setFocusable(true);
        b.setPadding(c.dp(20), 0, c.dp(20), 0);
        style(c, b, st, k);
        press(b);
        return b;
    }

    public static void style(Ctx c, TextView b, Style st, Icon.Kind k) {
        int fg = st == Style.PRIMARY ? c.p.onPrimary : st == Style.TONAL ? c.p.onPrimarySoft : c.p.primary;
        int bg = st == Style.PRIMARY ? c.p.primary : st == Style.TONAL ? c.p.primarySoft : 0;
        b.setTextColor(fg);
        if (st == Style.GHOST) b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x220F766E), null, rounded(c, Color.WHITE, 16)));
        else surface(c, b, bg, 16, false);
        if (k != null) {
            Drawable d = new Icon(k, fg); d.setBounds(0, 0, c.dp(20), c.dp(20));
            b.setCompoundDrawablesRelative(d, null, null, null); b.setCompoundDrawablePadding(c.dp(8));
        } else b.setCompoundDrawablesRelative(null, null, null, null);
    }

    /** Fade + rise entrance, used with a small per-item delay for staggered lists. */
    public static void appear(View v, long delayMs) {
        v.setAlpha(0f); v.setTranslationY(v.getResources().getDisplayMetrics().density * 22);
        v.animate().alpha(1f).translationY(0).setStartDelay(delayMs).setDuration(420).setInterpolator(EMPH).start();
    }

    /** Cross-fades a label's text with a tiny vertical slide. */
    public static void swapText(TextView t, CharSequence s) {
        if (t.getText().toString().contentEquals(s)) return;
        float d = t.getResources().getDisplayMetrics().density * 5;
        t.animate().alpha(0f).translationY(-d).setDuration(90).withEndAction(() -> {
            t.setText(s); t.setTranslationY(d);
            t.animate().alpha(1f).translationY(0).setDuration(160).setInterpolator(STD).start();
        }).start();
    }

    /** Animates a view's visibility with a slide from the bottom/top edge. */
    public static void slideVisible(View v, boolean show, int offsetPx) {
        v.animate().cancel();
        if (show) {
            if (v.getVisibility() != View.VISIBLE) { v.setAlpha(0f); v.setTranslationY(offsetPx); v.setVisibility(View.VISIBLE); }
            v.animate().alpha(1f).translationY(0).setDuration(320).setInterpolator(EMPH).start();
        } else if (v.getVisibility() == View.VISIBLE) {
            v.animate().alpha(0f).translationY(offsetPx).setDuration(200).setInterpolator(EXIT).withEndAction(() -> v.setVisibility(View.INVISIBLE)).start();
        }
    }

    public static LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }

    public static LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h); p.setMargins(l, t, r, b); return p;
    }

    public static ValueAnimator ticker(float from, float to, long ms, ValueAnimator.AnimatorUpdateListener l) {
        ValueAnimator a = ValueAnimator.ofFloat(from, to); a.setDuration(ms); a.addUpdateListener(l); return a;
    }
}
