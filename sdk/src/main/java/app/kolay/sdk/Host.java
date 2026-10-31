package app.kolay.sdk;

import android.app.Activity;
import java.io.File;
import java.util.function.Consumer;

/**
 * Shell -> payload contract: everything the stable shell offers the replaceable payload.
 *
 * Compatibility rule: this interface only ever GROWS. Never remove or change a method; add new
 * ones and raise {@link #API}. Payloads declare the API level they need in their manifest
 * ({@code app.kolay.api}); the shell refuses payloads that need more than it offers.
 */
public interface Host {
    /**
     * Contract version implemented by this shell.
     * 2: shell bundles ZXing core, declares the CAMERA permission and the quick-settings tile, forwards {@link Module#onPause()},
     *    and can download + install a newer shell ({@link #shellUpdateAvailable()}, {@link #installShellUpdate}).
     */
    int API = 2;

    /** The shell's activity: the payload builds its whole UI inside it. */
    Activity activity();

    // ---- secrets (device-specific, encrypted at rest, never part of the repo or any APK) ----

    /** @return the stored secret for {@code name}, or "" when unset. Re-read on every use. */
    String secret(String name);
    /** Stores a secret; a null/blank value deletes it. */
    void setSecret(String name, String value);

    // ---- platform resources owned by the shell ----

    /** Directory whose "tessdata/" subfolder holds deu.traineddata (what TessBaseAPI.init expects). */
    File tessDataParent();

    // ---- lifecycle / identity ----

    /** Payload calls this once its first screen is visible. Never calling it counts as a failed boot. */
    void ready();
    int payloadVersion();
    String payloadName();
    int shellVersion();

    // ---- privacy ----

    /** FLAG_SECURE: blocks screenshots and hides the app preview in the recents list. Persisted by the shell. */
    boolean secureWindow();
    void setSecureWindow(boolean on);

    // ---- updates ----

    /** Human-readable result of the last update check. */
    String updateStatus();
    /** True when a newer, verified payload is staged on disk and waits for {@link #restart()}. */
    boolean updateReady();
    /** True when the newest release needs a newer shell APK than the installed one. */
    boolean shellUpdateNeeded();
    /** Runs a check in the background; {@code done} receives the status text on the main thread. */
    void checkForUpdates(Consumer<String> done);
    /** {@code r} runs on the main thread whenever a new payload gets staged. Replaces any earlier listener. */
    void onUpdateReady(Runnable r);
    /** True when the newest release offers a signed shell APK newer than the installed one (API 2). */
    boolean shellUpdateAvailable();
    /**
     * Downloads, verifies and hands that shell APK to the system installer, which asks the user to confirm (API 2).
     * {@code progress} gets short status texts on the main thread. May first send the user to the "install unknown apps" setting.
     */
    void installShellUpdate(Consumer<String> progress);
    /** Re-creates the activity so the newest staged payload takes over. Call on the main thread. */
    void restart();
    String releasesUrl();
}
