package app.kolay.payload.tools.halal;

import android.graphics.Bitmap;
import android.view.*;
import android.widget.*;
import app.kolay.payload.tools.qr.QrCamera;
import app.kolay.payload.ui.*;

/** Step 2 (only when the barcode gave no ingredients): photograph the ingredient list, then crop it ({@link CropScreen}). The photo lives in memory only. */
final class LabelScreen extends CamScreen {
    private final String barcode, note;
    private TextView shoot;

    /** @param note why we are here / what to do, shown under the camera */
    LabelScreen(String barcode, String note) { this.barcode = barcode; this.note = note; }

    @Override String title() { return "İçindekiler"; }
    @Override String subtitle() { return "Yazıyı net ve düz çerçevele"; }
    @Override QrCamera.Mode mode() { return QrCamera.Mode.TEXT; }
    @Override boolean ocrMode() { return true; }

    @Override void buildFooter(LinearLayout f) {
        TextView t = Ui.text(c, note, 13, c.p.muted, 400);
        t.setGravity(Gravity.CENTER); t.setLineSpacing(0, 1.2f);
        f.addView(t, Ui.lp(-1, -2, 0, 0, 0, c.dp(10)));
        shoot = Ui.button(c, "Fotoğrafla ve sorgula", Ui.Style.PRIMARY, Icon.Kind.CAMERA);
        shoot.setOnClickListener(v -> {
            Ui.haptic(v, HapticFeedbackConstants.VIRTUAL_KEY);
            shoot.setEnabled(false);
            shoot.setText("Çekiliyor…");
            cam.capture();
        });
        f.addView(shoot, Ui.lp(-1, c.dp(Ui.H_BAR)));
    }

    @Override void leave() { c.nav.replace(new ScanScreen(null)); }

    @Override public void onStill(Bitmap still) {
        finish();
        Ui.haptic(footer, HapticFeedbackConstants.CONFIRM);
        c.nav.replace(new CropScreen(barcode, note, still));
    }
}
