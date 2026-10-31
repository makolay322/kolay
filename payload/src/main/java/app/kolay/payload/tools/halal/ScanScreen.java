package app.kolay.payload.tools.halal;

import android.view.*;
import android.widget.*;
import app.kolay.payload.tools.qr.QrCamera;
import app.kolay.payload.ui.*;

/** Step 1: read the product barcode, then look it up. */
final class ScanScreen extends CamScreen {
    @Override String title() { return "Helal Sorgu"; }
    @Override String subtitle() { return "Barkodu çerçeveye hizala"; }
    @Override QrCamera.Mode mode() { return QrCamera.Mode.PRODUCT; }

    @Override void buildFooter(LinearLayout f) {
        TextView t = Ui.text(c, "Ürün veritabanında yoksa içindekiler yazısını fotoğraflarsın.", 12, c.p.muted, 400);
        t.setGravity(Gravity.CENTER);
        f.addView(t, Ui.lp(-1, -2, 0, 0, 0, c.dp(6)));
        TextView photo = Ui.button(c, "Barkod yok — etiketi fotoğrafla", Ui.Style.GHOST, Icon.Kind.CAMERA);
        photo.setOnClickListener(v -> { finish(); c.nav.replace(new LabelScreen(null, "İçindekiler yazısını çerçeveye al")); });
        f.addView(photo, Ui.lp(-2, c.dp(44)));
    }

    @Override void leave() { c.nav.pop(); }

    @Override public void onCode(String code) {
        finish();
        Ui.haptic(footer, HapticFeedbackConstants.CONFIRM);
        c.nav.replace(new WorkScreen(code, null));
    }
}
