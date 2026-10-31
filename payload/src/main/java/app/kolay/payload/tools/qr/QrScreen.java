package app.kolay.payload.tools.qr;

import android.Manifest;
import android.app.StatusBarManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import app.kolay.payload.ui.*;

/**
 * Camera QR reader. Starts scanning as soon as the screen is shown, stops the camera whenever the app leaves the foreground,
 * and never opens a link by itself: the decoded text is shown first. The decoded text lives only in memory.
 */
final class QrScreen extends Screen {
    private static final int REQ_CAMERA = 71;

    private QrCamera cam;
    private FrameLayout stage, cameraCard;
    private LinearLayout statePanel, footer;
    private ScrollView resultPanel;
    private TextView stateTitle, stateText, stateBtn, tileBtn;
    private FrameLayout torchBtn;
    private ImageView torchIcon;
    private boolean shown, found, asked;

    @Override public View create() {
        Palette p = c.p;
        LinearLayout page = new LinearLayout(c.a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.addView(new Header(c, "QR Okuyucu", "Kodu çerçeveye hizala", () -> { cam.stop(); c.nav.pop(); }).view);

        stage = new FrameLayout(c.a);
        stage.setPadding(c.dp(16), c.dp(4), c.dp(16), c.dp(12));

        // camera card
        cameraCard = new FrameLayout(c.a);
        Ui.surface(c, cameraCard, p.surface2, 22, false);
        cameraCard.setClipToOutline(true);
        TextureView tv = new TextureView(c.a);
        cameraCard.addView(tv, new FrameLayout.LayoutParams(-1, -1));
        cameraCard.addView(new Viewfinder(), new FrameLayout.LayoutParams(-1, -1));
        cam = new QrCamera(c.a, tv, new QrCamera.Listener() {
            @Override public void onCode(String text) { showResult(text); }
            @Override public void onError(String message) { showState("Kamera kullanılamıyor", message, "Tekrar dene", QrScreen.this::ensureCamera); }
        });
        if (cam.hasTorch()) {
            torchBtn = new FrameLayout(c.a);
            torchIcon = new ImageView(c.a);
            torchBtn.addView(torchIcon, new FrameLayout.LayoutParams(c.dp(24), c.dp(24), Gravity.CENTER));
            torchBtn.setContentDescription("Fener"); torchBtn.setClickable(true);
            paintTorch(false);
            torchBtn.setOnClickListener(v -> {
                Ui.haptic(v, HapticFeedbackConstants.VIRTUAL_KEY);
                cam.setTorch(!cam.torchOn());
                paintTorch(cam.torchOn());
            });
            Ui.press(torchBtn);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(c.dp(52), c.dp(52), Gravity.BOTTOM | Gravity.END);
            lp.setMargins(0, 0, c.dp(16), c.dp(16));
            cameraCard.addView(torchBtn, lp);
        }
        stage.addView(cameraCard, new FrameLayout.LayoutParams(-1, -1));

        // permission / error state
        statePanel = new LinearLayout(c.a);
        statePanel.setOrientation(LinearLayout.VERTICAL);
        statePanel.setGravity(Gravity.CENTER);
        statePanel.setPadding(c.dp(24), 0, c.dp(24), 0);
        FrameLayout badge = new FrameLayout(c.a);
        badge.setBackground(Ui.rounded(c, p.primarySoft, 22));
        badge.addView(Ui.icon(c, Icon.Kind.QR, p.onPrimarySoft, 32), new FrameLayout.LayoutParams(c.dp(32), c.dp(32), Gravity.CENTER));
        statePanel.addView(badge, new LinearLayout.LayoutParams(c.dp(72), c.dp(72)));
        stateTitle = Ui.text(c, "", 18, p.ink, 600);
        stateTitle.setGravity(Gravity.CENTER);
        statePanel.addView(stateTitle, Ui.lp(-2, -2, 0, c.dp(20), 0, 0));
        stateText = Ui.text(c, "", 14, p.muted, 400);
        stateText.setGravity(Gravity.CENTER); stateText.setLineSpacing(0, 1.25f);
        statePanel.addView(stateText, Ui.lp(-2, -2, 0, c.dp(8), 0, 0));
        stateBtn = Ui.button(c, "", Ui.Style.PRIMARY, null);
        statePanel.addView(stateBtn, Ui.lp(-2, c.dp(52), 0, c.dp(24), 0, 0));
        statePanel.setVisibility(View.GONE);
        stage.addView(statePanel, new FrameLayout.LayoutParams(-1, -1));

        // result
        resultPanel = new ScrollView(c.a);
        resultPanel.setVerticalScrollBarEnabled(false);
        resultPanel.setVisibility(View.GONE);
        stage.addView(resultPanel, new FrameLayout.LayoutParams(-1, -1));

        page.addView(stage, new LinearLayout.LayoutParams(-1, 0, 1));

        // footer: add the quick-settings tile (Android 13+ can ask the system to add it with one tap)
        footer = new LinearLayout(c.a);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(c.dp(16), 0, c.dp(16), c.dp(14));
        if (Build.VERSION.SDK_INT >= 33) {
            tileBtn = Ui.button(c, "Hızlı ayarlara ekle", Ui.Style.GHOST, Icon.Kind.QR);
            tileBtn.setOnClickListener(v -> addTile());
            footer.addView(tileBtn, new LinearLayout.LayoutParams(-2, c.dp(44)));
        } else {
            TextView t = Ui.text(c, "Bildirim panelinin düzenleme ekranından “QR Okuyucu” kutucuğunu ekleyebilirsin.", 12, p.muted, 400);
            t.setGravity(Gravity.CENTER);
            footer.addView(t);
        }
        page.addView(footer);
        return page;
    }

    // ---- lifecycle ------------------------------------------------------------------------------

    @Override public void onShown() { shown = true; ensureCamera(); }
    @Override public void onResume() { if (shown) ensureCamera(); }
    @Override public void onPause() { cam.stop(); paintTorch(false); }
    @Override public void onDestroy() { cam.stop(); }
    @Override public boolean onBack() { cam.stop(); return false; }  // release the camera right away; Nav then pops

    private boolean granted() { return c.a.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED; }

    private void ensureCamera() {
        if (found || !shown) return;
        if (granted()) {
            statePanel.setVisibility(View.GONE);
            cameraCard.setVisibility(View.VISIBLE);
            footer.setVisibility(View.VISIBLE);
            cam.start();
        } else if (!asked) {
            asked = true;
            c.a.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        } else {
            showState("Kamera izni gerekli", "QR kodu okumak için kameraya erişim izni ver.",
                c.a.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ? "İzin ver" : "Ayarları aç", this::askOrSettings);
        }
    }

    private void askOrSettings() {
        if (c.a.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            c.a.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        } else {
            c.a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", c.a.getPackageName(), null)));
        }
    }

    @Override public void onPermission(int req, String[] perms, int[] results) {
        if (req == REQ_CAMERA) ensureCamera();
    }

    private void showState(String title, String text, String btn, Runnable action) {
        cam.stop();
        cameraCard.setVisibility(View.GONE);
        footer.setVisibility(View.GONE);
        resultPanel.setVisibility(View.GONE);
        stateTitle.setText(title); stateText.setText(text); stateBtn.setText(btn);
        stateBtn.setOnClickListener(v -> action.run());
        if (statePanel.getVisibility() != View.VISIBLE) { statePanel.setVisibility(View.VISIBLE); Ui.appear(statePanel, 0); }
    }

    // ---- result ---------------------------------------------------------------------------------

    private static boolean isLink(String s) {
        String l = s.trim().toLowerCase(java.util.Locale.ROOT);
        return (l.startsWith("https://") || l.startsWith("http://")) && l.indexOf(' ') < 0 && l.indexOf('\n') < 0;
    }

    private void showResult(String text) {
        found = true;
        cam.stop();
        paintTorch(false);
        Palette p = c.p;
        boolean link = isLink(text);
        Ui.haptic(stage, HapticFeedbackConstants.CONFIRM);

        LinearLayout col = new LinearLayout(c.a);
        col.setOrientation(LinearLayout.VERTICAL);

        TextView label = Ui.text(c, link ? "BAĞLANTI" : "METİN", 12, p.muted, 600);
        label.setLetterSpacing(0.1f);
        col.addView(label, Ui.lp(-2, -2, c.dp(4), 0, 0, c.dp(10)));

        LinearLayout card = new LinearLayout(c.a);
        card.setPadding(c.dp(18), c.dp(18), c.dp(18), c.dp(18));
        Ui.surface(c, card, p.surface, 22, true);
        TextView t = Ui.text(c, text, 16, p.ink, 400);
        t.setLineSpacing(0, 1.3f);
        card.addView(t);
        col.addView(card, Ui.lp(-1, -2));

        if (link) {
            TextView open = Ui.button(c, "Bağlantıyı aç", Ui.Style.PRIMARY, Icon.Kind.LINK);
            open.setOnClickListener(v -> openLink(text));
            col.addView(open, Ui.lp(-1, c.dp(56), 0, c.dp(16), 0, 0));
        }
        LinearLayout row = new LinearLayout(c.a);
        TextView copy = Ui.button(c, "Kopyala", link ? Ui.Style.TONAL : Ui.Style.PRIMARY, Icon.Kind.COPY);
        TextView share = Ui.button(c, "Paylaş", Ui.Style.TONAL, Icon.Kind.SHARE);
        copy.setOnClickListener(v -> copy(copy, text, link));
        share.setOnClickListener(v -> {
            Ui.haptic(v, HapticFeedbackConstants.VIRTUAL_KEY);
            c.a.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null));
        });
        LinearLayout.LayoutParams l1 = Ui.lp(0, c.dp(56), 0, 0, c.dp(10), 0); l1.weight = 1;
        LinearLayout.LayoutParams l2 = Ui.lp(0, c.dp(56)); l2.weight = 1;
        row.addView(copy, l1); row.addView(share, l2);
        col.addView(row, Ui.lp(-1, -2, 0, c.dp(12), 0, 0));

        TextView again = Ui.button(c, "Yeniden tara", Ui.Style.GHOST, Icon.Kind.QR);
        again.setOnClickListener(v -> rescan());
        col.addView(again, Ui.lp(-1, c.dp(52), 0, c.dp(12), 0, 0));

        resultPanel.removeAllViews();
        resultPanel.addView(col);
        resultPanel.scrollTo(0, 0);
        cameraCard.setVisibility(View.GONE);
        footer.setVisibility(View.GONE);
        statePanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.VISIBLE);
        Ui.appear(resultPanel, 0);
    }

    private void rescan() {
        found = false;
        resultPanel.removeAllViews();
        resultPanel.setVisibility(View.GONE);
        ensureCamera();
    }

    /** Only http(s) links are ever handed to the system, and only after the user taps. */
    private void openLink(String text) {
        try {
            c.a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(text.trim())));
        } catch (ActivityNotFoundException ignored) { }
    }

    private void copy(TextView btn, String text, boolean link) {
        ClipData clip = ClipData.newPlainText("QR", text);
        PersistableBundle extras = new PersistableBundle();
        extras.putBoolean("android.content.extra.IS_SENSITIVE", true);  // a code may hold a Wi-Fi password or token: keep it out of the clipboard preview
        clip.getDescription().setExtras(extras);
        ((ClipboardManager) c.a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(clip);
        Ui.haptic(btn, HapticFeedbackConstants.CONFIRM);
        Ui.Style st = link ? Ui.Style.TONAL : Ui.Style.PRIMARY;
        btn.setText("Kopyalandı");
        Ui.style(c, btn, st, Icon.Kind.CHECK);
        c.ui.postDelayed(() -> {
            if (btn.isAttachedToWindow()) { btn.setText("Kopyala"); Ui.style(c, btn, st, Icon.Kind.COPY); }
        }, 1600);
    }

    // ---- torch + quick-settings tile ------------------------------------------------------------

    private void paintTorch(boolean on) {
        if (torchBtn == null) return;
        Palette p = c.p;
        torchBtn.setBackground(Ui.rounded(c, on ? p.primary : 0x99000000, 26));
        torchIcon.setImageDrawable(new Icon(Icon.Kind.FLASH, on ? p.onPrimary : Color.WHITE));
    }

    /** Asks the system to add the shell's tile to the pull-down panel (Android 13+). The user confirms in a system dialog. */
    private void addTile() {
        if (Build.VERSION.SDK_INT < 33) return;
        try {
            StatusBarManager sbm = c.a.getSystemService(StatusBarManager.class);
            int res = c.a.getResources().getIdentifier("ic_qr_tile", "drawable", c.a.getPackageName());
            if (sbm == null || res == 0) { Ui.swapText(tileBtn, "Bu cihazda eklenemedi"); return; }
            sbm.requestAddTileService(new ComponentName(c.a.getPackageName(), "app.kolay.shell.QrTileService"), "QR Okuyucu",
                android.graphics.drawable.Icon.createWithResource(c.a, res), c.a.getMainExecutor(), result -> {
                    if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) Ui.swapText(tileBtn, "Zaten ekli");
                    else if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) Ui.swapText(tileBtn, "Eklendi");
                });
        } catch (Exception e) {
            Ui.swapText(tileBtn, "Bu cihazda eklenemedi");
        }
    }

    // ---- viewfinder -----------------------------------------------------------------------------

    /** Four white corner brackets around a centered square; purely a visual aim guide (the whole frame is decoded). */
    private final class Viewfinder extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Viewfinder() {
            super(c.a);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(c.dp(4)); paint.setColor(Color.WHITE);
        }
        @Override protected void onDraw(Canvas cv) {
            float side = Math.min(getWidth(), getHeight()) * 0.66f, arm = side * 0.18f;
            float l = (getWidth() - side) / 2f, t = (getHeight() - side) / 2f, r = l + side, b = t + side;
            cv.drawLines(new float[]{
                l, t + arm, l, t,  l, t, l + arm, t,
                r - arm, t, r, t,  r, t, r, t + arm,
                r, b - arm, r, b,  r, b, r - arm, b,
                l + arm, b, l, b,  l, b, l, b - arm}, paint);
        }
    }
}
