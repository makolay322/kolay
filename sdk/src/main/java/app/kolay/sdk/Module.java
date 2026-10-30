package app.kolay.sdk;

import android.os.Bundle;

/**
 * The replaceable part of the app. A payload APK ships a public class
 * {@code app.kolay.payload.Entry} with a public no-arg constructor implementing this.
 *
 * Compatibility rule: new callbacks are only ever added as {@code default} methods, so payloads
 * built against an older contract keep linking.
 */
public interface Module {
    void onCreate(Host host, Bundle saved);
    default void onResume() {}
    default void onPermissionResult(int req, String[] perms, int[] results) {}
    /** @return true if the payload consumed the back press. */
    default boolean onBackPressed() { return false; }
    default void onDestroy() {}
}
