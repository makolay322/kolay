package app.kolay.shell;

import android.app.Activity;
import java.io.File;
import java.util.function.Consumer;

/**
 * What the platform layer ({@code app.kolay.shell}: activity, updater, key store, prayer engine) offers the UI and the tools
 * ({@code app.kolay.app}). One APK, one build: this is an ordinary internal interface, free to change.
 */
public interface Host {
    /** The activity the whole UI is built in. */
    Activity activity();

    // ---- secrets (device-specific, encrypted at rest, never part of the repo or the APK) ----

    /** @return the stored secret for {@code name}, or "" when unset. Re-read on every use. */
    String secret(String name);
    /** Stores a secret; a null/blank value deletes it. */
    void setSecret(String name, String value);

    /** Directory whose "tessdata/" subfolder holds deu.traineddata (what TessBaseAPI.init expects). */
    File tessDataParent();

    /** Installed version name, e.g. "1.014". */
    String versionName();

    // ---- self-update ----

    /** Human-readable result of the last update check. */
    String updateStatus();
    /** True when a newer, verified Kolay.apk has been downloaded and waits for {@link #installUpdate}. */
    boolean updateReady();
    /** Version name of that downloaded update ("" when none). */
    String updateName();
    /** Runs a check (and download) in the background; {@code done} receives the status text on the main thread. */
    void checkForUpdates(Consumer<String> done);
    /** {@code r} runs on the main thread whenever an update has been downloaded. Replaces any earlier listener; null clears. */
    void onUpdateReady(Runnable r);
    /**
     * Hands the downloaded update to Android's installer. The first time Android may ask to confirm; afterwards
     * (Android 12+) Kolay usually updates itself without a prompt. Installing closes the app. {@code progress} gets short
     * status texts on the main thread; it may first send the user to the "install unknown apps" setting.
     */
    void installUpdate(Consumer<String> progress);
    String releasesUrl();

    // ---- prayer times (engine, widget and alarms live in app.kolay.shell so they work while the UI is closed) ----

    /** @return {latitude, longitude} saved by {@link #prayerSetLocation}, or null when none yet */
    double[] prayerLocation();
    /** Saves the location (kept on the device) and repaints the widget / re-arms the reminder. */
    void prayerSetLocation(double lat, double lon);
    /** @return 6 epoch-ms instants (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha) for the local date (month 1-12), or null without a location */
    long[] prayerTimes(int year, int month, int day);
    /** Which prayers have the reminder on: bit i = prayer i (0 Sabah, 2 Öğle, 3 İkindi, 4 Akşam, 5 Yatsı; bit 1 unused). */
    int prayerReminderMask();
    void prayerSetReminderMask(int mask);
    /** Asks the launcher to add the prayer widget to the home screen; false when the launcher cannot do that. */
    boolean prayerPinWidget();
}
