package app.kolay.app.tools.halal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.*;
import android.widget.*;
import app.kolay.app.ui.*;

/**
 * Step 2b: the photographed label is shown with an adjustable frame so only the ingredient list goes to OCR
 * (surrounding text makes the input noisy). The bitmap lives in memory only; whoever receives it owns it.
 */
final class CropScreen extends Screen {
    private final String barcode, note;
    private Bitmap still;       // owned until handed to WorkScreen or recycled
    private boolean handedOver;

    CropScreen(String barcode, String note, Bitmap still) { this.barcode = barcode; this.note = note; this.still = still; }

    @Override public View create() {
        Palette p = c.p;
        LinearLayout col = new LinearLayout(c.a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(new Header(c, "Yazıyı seç", "Yalnızca içindekiler kısmını çerçevele", this::retake).view);

        CropView cv = new CropView(c.a, still, c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 0, 1);
        lp.setMargins(c.dp(Ui.GUTTER), 0, c.dp(Ui.GUTTER), c.dp(8));
        col.addView(cv, lp);

        LinearLayout f = new LinearLayout(c.a);
        f.setOrientation(LinearLayout.VERTICAL);
        f.setPadding(c.dp(Ui.GUTTER), c.dp(4), c.dp(Ui.GUTTER), c.dp(Ui.GUTTER));
        TextView hint = Ui.text(c, "Köşeleri ve kenarları sürükle; çerçeveyi içinden kaydır.", 13, p.muted, 400);
        hint.setGravity(Gravity.CENTER);
        f.addView(hint, Ui.lp(-1, -2, 0, 0, 0, c.dp(10)));
        TextView go = Ui.button(c, "Seçimi sorgula", Ui.Style.PRIMARY, Icon.Kind.SCAN);
        go.setOnClickListener(v -> {
            Ui.haptic(v, HapticFeedbackConstants.VIRTUAL_KEY);
            Bitmap part = cv.cropped();
            if (part == null) return;
            handedOver = true;
            Bitmap whole = still; still = null;
            if (whole != null && whole != part) whole.recycle();
            c.nav.replace(new WorkScreen(barcode, part));
        });
        f.addView(go, Ui.lp(-1, c.dp(Ui.H_BAR)));
        TextView again = Ui.button(c, "Yeniden çek", Ui.Style.GHOST, Icon.Kind.CAMERA);
        again.setOnClickListener(v -> retake());
        f.addView(again, Ui.lp(-1, c.dp(Ui.H_GHOST), 0, c.dp(6), 0, 0));
        col.addView(f, new LinearLayout.LayoutParams(-1, -2));
        return col;
    }

    private void retake() { c.nav.replace(new LabelScreen(barcode, note)); }

    @Override public boolean onBack() { retake(); return true; }

    @Override public void onDestroy() {
        if (!handedOver && still != null) { still.recycle(); still = null; }
    }

    /** Bitmap with a draggable frame: drag a corner/edge to resize, drag inside to move. */
    private static final class CropView extends View {
        private final Bitmap bmp;
        private final RectF crop = new RectF();          // image coordinates
        private final Paint img = new Paint(Paint.FILTER_BITMAP_FLAG), dim = new Paint(), line = new Paint(Paint.ANTI_ALIAS_FLAG), dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float slop, minPx, dotR;
        private float scale = 1, ox, oy, lastX, lastY;
        private int mode;                                // bit mask: 1 left, 2 top, 4 right, 8 bottom, 16 move
        private static final int L = 1, T = 2, R = 4, B = 8, MOVE = 16;

        CropView(Context ctx, Bitmap b, Ctx c) {
            super(ctx);
            bmp = b;
            slop = c.dp(30); minPx = c.dp(56); dotR = c.dp(6);
            dim.setColor(0x99000000);
            line.setStyle(Paint.Style.STROKE); line.setStrokeWidth(c.dp(2)); line.setColor(c.p.primary);
            dot.setColor(c.p.primary);
            float mx = b.getWidth() * 0.06f, my = b.getHeight() * 0.25f;
            crop.set(mx, my, b.getWidth() - mx, b.getHeight() - my);
        }

        private void fit() {
            float w = getWidth(), h = getHeight();
            scale = Math.min(w / bmp.getWidth(), h / bmp.getHeight());
            ox = (w - bmp.getWidth() * scale) / 2f; oy = (h - bmp.getHeight() * scale) / 2f;
        }

        private RectF view() { return new RectF(ox + crop.left * scale, oy + crop.top * scale, ox + crop.right * scale, oy + crop.bottom * scale); }

        @Override protected void onDraw(Canvas cv) {
            fit();
            cv.save(); cv.translate(ox, oy); cv.scale(scale, scale); cv.drawBitmap(bmp, 0, 0, img); cv.restore();
            RectF r = view();
            float w = getWidth(), h = getHeight();
            cv.drawRect(0, 0, w, r.top, dim); cv.drawRect(0, r.bottom, w, h, dim);
            cv.drawRect(0, r.top, r.left, r.bottom, dim); cv.drawRect(r.right, r.top, w, r.bottom, dim);
            cv.drawRect(r, line);
            float mx = (r.left + r.right) / 2, my = (r.top + r.bottom) / 2;
            for (float[] d : new float[][]{{r.left, r.top}, {r.right, r.top}, {r.left, r.bottom}, {r.right, r.bottom}, {mx, r.top}, {mx, r.bottom}, {r.left, my}, {r.right, my}})
                cv.drawCircle(d[0], d[1], dotR, dot);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            float x = e.getX(), y = e.getY();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    RectF r = view();
                    boolean inX = x > r.left - slop && x < r.right + slop, inY = y > r.top - slop && y < r.bottom + slop;
                    mode = 0;
                    if (inY && Math.abs(x - r.left) < slop) mode |= L;
                    if (inY && Math.abs(x - r.right) < slop) mode |= R;
                    if (inX && Math.abs(y - r.top) < slop) mode |= T;
                    if (inX && Math.abs(y - r.bottom) < slop) mode |= B;
                    if ((mode & (L | R)) == (L | R)) mode &= ~(x - r.left < r.right - x ? R : L);   // very narrow frame: pick the nearer edge
                    if ((mode & (T | B)) == (T | B)) mode &= ~(y - r.top < r.bottom - y ? B : T);
                    if (mode == 0 && r.contains(x, y)) mode = MOVE;
                    lastX = x; lastY = y;
                    if (mode != 0) getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (mode == 0 || scale <= 0) return true;
                    float dx = (x - lastX) / scale, dy = (y - lastY) / scale;
                    lastX = x; lastY = y;
                    float bw = bmp.getWidth(), bh = bmp.getHeight();
                    float min = Math.min(minPx / scale, Math.min(bw, bh) / 2f);
                    if (mode == MOVE) {
                        dx = Math.max(-crop.left, Math.min(bw - crop.right, dx));
                        dy = Math.max(-crop.top, Math.min(bh - crop.bottom, dy));
                        crop.offset(dx, dy);
                    } else {
                        if ((mode & L) != 0) crop.left = Math.max(0, Math.min(crop.right - min, crop.left + dx));
                        if ((mode & R) != 0) crop.right = Math.min(bw, Math.max(crop.left + min, crop.right + dx));
                        if ((mode & T) != 0) crop.top = Math.max(0, Math.min(crop.bottom - min, crop.top + dy));
                        if ((mode & B) != 0) crop.bottom = Math.min(bh, Math.max(crop.top + min, crop.bottom + dy));
                    }
                    invalidate();
                    return true;
                }
                case MotionEvent.ACTION_UP: case MotionEvent.ACTION_CANCEL:
                    mode = 0; return true;
            }
            return true;
        }

        /** A copy of the framed part (the caller owns it), or null if the frame is empty. */
        Bitmap cropped() {
            int l = Math.max(0, Math.round(crop.left)), t = Math.max(0, Math.round(crop.top));
            int w = Math.min(bmp.getWidth() - l, Math.round(crop.width())), h = Math.min(bmp.getHeight() - t, Math.round(crop.height()));
            if (w < 8 || h < 8) return null;
            return Bitmap.createBitmap(bmp, l, t, w, h);
        }
    }
}
