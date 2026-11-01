package app.kolay.payload.ui;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Stroke icons on a 24-unit grid, drawn with paths so they stay crisp at any size and take any color. */
public final class Icon extends Drawable {
    public enum Kind { BACK, CHEVRON, CHECK, COPY, SHARE, SETTINGS, SCAN, SHIELD, KEY, REFRESH, LINK, QR, FLASH, BARCODE, CAMERA, CRESCENT, CLOCK }

    private final Kind kind;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public Icon(Kind kind, int color) {
        this.kind = kind;
        p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND); p.setColor(color);
    }

    @Override public void draw(Canvas c) {
        Rect b = getBounds();
        float s = b.width() / 24f;
        c.save(); c.translate(b.left, b.top); c.scale(s, s);
        p.setStrokeWidth(2f);
        path.reset();
        switch (kind) {
            case BACK: path.moveTo(15, 5); path.lineTo(8, 12); path.lineTo(15, 19); break;
            case CHEVRON: path.moveTo(9, 5); path.lineTo(16, 12); path.lineTo(9, 19); break;
            case CHECK: path.moveTo(5, 12.5f); path.lineTo(10, 17.5f); path.lineTo(19, 7); break;
            case COPY:
                path.addRoundRect(9, 9, 20, 20, 2.5f, 2.5f, Path.Direction.CW);
                path.moveTo(15, 9); path.lineTo(15, 6.5f); path.quadTo(15, 4, 12.5f, 4); path.lineTo(6.5f, 4);
                path.quadTo(4, 4, 4, 6.5f); path.lineTo(4, 12.5f); path.quadTo(4, 15, 6.5f, 15); path.lineTo(9, 15);
                break;
            case SHARE:
                path.addCircle(18, 5.5f, 2.6f, Path.Direction.CW); path.addCircle(6, 12, 2.6f, Path.Direction.CW); path.addCircle(18, 18.5f, 2.6f, Path.Direction.CW);
                path.moveTo(8.2f, 10.8f); path.lineTo(15.8f, 6.7f); path.moveTo(8.2f, 13.2f); path.lineTo(15.8f, 17.3f);
                break;
            case SETTINGS:
                slider(7, 15); slider(12, 9); slider(17, 16);
                break;
            case SCAN:
                path.moveTo(4, 8.5f); path.lineTo(4, 6.5f); path.quadTo(4, 4, 6.5f, 4); path.lineTo(8.5f, 4);
                path.moveTo(15.5f, 4); path.lineTo(17.5f, 4); path.quadTo(20, 4, 20, 6.5f); path.lineTo(20, 8.5f);
                path.moveTo(20, 15.5f); path.lineTo(20, 17.5f); path.quadTo(20, 20, 17.5f, 20); path.lineTo(15.5f, 20);
                path.moveTo(8.5f, 20); path.lineTo(6.5f, 20); path.quadTo(4, 20, 4, 17.5f); path.lineTo(4, 15.5f);
                path.moveTo(8, 12); path.lineTo(16, 12);
                break;
            case SHIELD:
                path.moveTo(12, 3); path.lineTo(19, 6); path.lineTo(19, 11.5f);
                path.cubicTo(19, 16, 16, 19.5f, 12, 21); path.cubicTo(8, 19.5f, 5, 16, 5, 11.5f); path.lineTo(5, 6); path.close();
                break;
            case KEY:
                path.addCircle(8, 15.5f, 3.6f, Path.Direction.CW);
                path.moveTo(10.6f, 12.9f); path.lineTo(19, 4.5f); path.moveTo(16, 7.5f); path.lineTo(18.5f, 10);
                break;
            case REFRESH:
                path.addArc(new RectF(5, 5, 19, 19), -50, 280);
                path.moveTo(16.5f, 3.5f); path.lineTo(17.2f, 6.9f); path.lineTo(13.8f, 7.6f);
                break;
            case QR:
                path.addRoundRect(4, 4, 10, 10, 1.5f, 1.5f, Path.Direction.CW);
                path.addRoundRect(14, 4, 20, 10, 1.5f, 1.5f, Path.Direction.CW);
                path.addRoundRect(4, 14, 10, 20, 1.5f, 1.5f, Path.Direction.CW);
                path.moveTo(14, 14); path.lineTo(14, 20); path.moveTo(17, 14); path.lineTo(20, 14);
                path.moveTo(17, 17.5f); path.lineTo(20, 17.5f); path.moveTo(17.5f, 20); path.lineTo(20, 20);
                break;
            case FLASH:
                path.moveTo(13, 3); path.lineTo(6, 13.5f); path.lineTo(12, 13.5f); path.lineTo(11, 21); path.lineTo(18, 10.5f); path.lineTo(12, 10.5f); path.close();
                break;
            case BARCODE:
                for (int x = 4; x <= 20; x += 4) { path.moveTo(x, 5); path.lineTo(x, 19); }
                break;
            case CAMERA:
                path.addRoundRect(3.5f, 7, 20.5f, 19.5f, 3, 3, Path.Direction.CW);
                path.addCircle(12, 13.2f, 3.3f, Path.Direction.CW);
                path.moveTo(8.5f, 7); path.lineTo(9.8f, 4.5f); path.lineTo(14.2f, 4.5f); path.lineTo(15.5f, 7);
                break;
            case CRESCENT: {
                Path o = new Path(), i = new Path();
                o.addCircle(12, 12, 8, Path.Direction.CW); i.addCircle(16, 9.5f, 6.6f, Path.Direction.CW);
                o.op(i, Path.Op.DIFFERENCE); path.set(o);
                break;
            }
            case CLOCK:
                path.addCircle(12, 12, 8.5f, Path.Direction.CW);
                path.moveTo(12, 7); path.lineTo(12, 12); path.lineTo(15.5f, 14);
                break;
            case LINK:
                path.moveTo(10, 14); path.lineTo(14, 10);
                path.moveTo(8.5f, 11.5f); path.lineTo(6.8f, 13.2f); path.quadTo(5, 15.5f, 7, 17.3f); path.quadTo(9, 19, 10.8f, 17.2f); path.lineTo(12, 16);
                path.moveTo(15.5f, 12.5f); path.lineTo(17.2f, 10.8f); path.quadTo(19, 8.5f, 17, 6.7f); path.quadTo(15, 5, 13.2f, 6.8f); path.lineTo(12, 8);
                break;
        }
        c.drawPath(path, p);
        c.restore();
    }

    /** Horizontal line at y with a ring at x, leaving a gap around the ring. */
    private void slider(float y, float x) {
        path.moveTo(4, y); path.lineTo(x - 3.2f, y);
        path.moveTo(x + 3.2f, y); path.lineTo(20, y);
        path.addCircle(x, y, 2.2f, Path.Direction.CW);
    }

    @Override public void setAlpha(int a) { p.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter f) { p.setColorFilter(f); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    @Override public int getIntrinsicWidth() { return 24; }
    @Override public int getIntrinsicHeight() { return 24; }
}
