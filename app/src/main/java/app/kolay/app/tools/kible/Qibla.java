package app.kolay.app.tools.kible;

/** Great-circle direction and distance to the Kaaba. Pure Java (no Android), so it can be checked on a desktop JVM. */
public final class Qibla {
    private Qibla() {}

    public static final double KAABA_LAT = 21.422487, KAABA_LON = 39.826206;
    private static final double EARTH_KM = 6371.0088;

    /** Initial bearing from (lat, lon) to the Kaaba, degrees clockwise from true north, in [0, 360). */
    public static double bearing(double lat, double lon) {
        double p1 = Math.toRadians(lat), p2 = Math.toRadians(KAABA_LAT), dl = Math.toRadians(KAABA_LON - lon);
        double y = Math.sin(dl) * Math.cos(p2);
        double x = Math.cos(p1) * Math.sin(p2) - Math.sin(p1) * Math.cos(p2) * Math.cos(dl);
        return norm(Math.toDegrees(Math.atan2(y, x)));
    }

    /** Haversine distance to the Kaaba in km. */
    public static double distanceKm(double lat, double lon) {
        double p1 = Math.toRadians(lat), p2 = Math.toRadians(KAABA_LAT);
        double dp = p2 - p1, dl = Math.toRadians(KAABA_LON - lon);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2) + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * EARTH_KM * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    public static double norm(double deg) { deg %= 360; return deg < 0 ? deg + 360 : deg; }

    /** Signed smallest turn from {@code from} to {@code to}: positive = clockwise (turn right), in (-180, 180]. */
    public static double turn(double from, double to) {
        double d = norm(to - from);
        return d > 180 ? d - 360 : d;
    }
}
