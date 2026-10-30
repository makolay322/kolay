package app.kolay.payload.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Indeterminate loader: a rotating arc whose length breathes between short and long. */
public final class Arc extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG), arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private float rot, sweep = 40;
    private ValueAnimator spin, breathe;

    public Arc(Context c, int trackColor, int arcColor, float strokePx) {
        super(c);
        for (Paint p : new Paint[]{track, arc}) { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(strokePx); p.setStrokeCap(Paint.Cap.ROUND); }
        track.setColor(trackColor); arc.setColor(arcColor);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        spin = ValueAnimator.ofFloat(0, 360); spin.setDuration(1300); spin.setRepeatCount(ValueAnimator.INFINITE); spin.setInterpolator(new LinearInterpolator());
        spin.addUpdateListener(a -> { rot = (float) a.getAnimatedValue(); invalidate(); });
        breathe = ValueAnimator.ofFloat(30, 250); breathe.setDuration(1100); breathe.setRepeatCount(ValueAnimator.INFINITE); breathe.setRepeatMode(ValueAnimator.REVERSE);
        breathe.setInterpolator(Ui.STD);
        breathe.addUpdateListener(a -> sweep = (float) a.getAnimatedValue());
        spin.start(); breathe.start();
    }

    @Override protected void onDetachedFromWindow() {
        if (spin != null) spin.cancel();
        if (breathe != null) breathe.cancel();
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas c) {
        float inset = arc.getStrokeWidth() / 2f;
        box.set(inset, inset, getWidth() - inset, getHeight() - inset);
        c.drawOval(box, track);
        c.drawArc(box, rot - 90, sweep, false, arc);
    }
}
