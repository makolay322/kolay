package app.kolay.payload.tools.halal;

import android.view.*;
import android.widget.*;
import app.kolay.payload.tools.qr.QrCamera;
import app.kolay.payload.ui.*;

/** Step 1: read the product barcode, then look it up. Barcode is the default mode; OCR is only entered through the mode switch. */
final class ScanScreen extends CamScreen {
    private final String note;   // why we are back here (e.g. barcode not in the database), or null

    ScanScreen(String note) { this.note = note; }

    @Override String title() { return "Helal Sorgu"; }
    @Override String subtitle() { return "Barkodu çerçeveye hizala"; }
    @Override QrCamera.Mode mode() { return QrCamera.Mode.PRODUCT; }
    @Override boolean ocrMode() { return false; }

    @Override void buildFooter(LinearLayout f) {
        TextView t = Ui.text(c, note != null ? note : "Barkod okunmazsa üstten «Etiket (OCR)» seçeneğine geçip içindekiler yazısını fotoğraflayabilirsin.", 13,
            note != null ? c.p.ink : c.p.muted, note != null ? 500 : 400);
        t.setGravity(Gravity.CENTER); t.setLineSpacing(0, 1.2f);
        f.addView(t, Ui.lp(-1, -2, 0, 0, 0, c.dp(4)));
    }

    @Override void leave() { c.nav.pop(); }

    @Override public void onCode(String code) {
        finish();
        Ui.haptic(footer, HapticFeedbackConstants.CONFIRM);
        c.nav.replace(new WorkScreen(code, null));
    }
}
