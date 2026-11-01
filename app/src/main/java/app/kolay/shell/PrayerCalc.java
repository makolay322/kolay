package app.kolay.shell;

import java.util.TimeZone;

/**
 * Astronomical prayer times (the usual solar-position method). Pure Java, no Android types, so it can be run on a JVM.
 * Angles follow the Diyanet convention (Fajr 18, Isha 17); Asr is "asr-ı evvel" (shadow factor 1), as in the Diyanet calendar.
 * At high latitudes, where the twilight angle is never reached, Fajr/Isha fall back to the angle-based night portion.
 */
final class PrayerCalc {
    private PrayerCalc() {}

    static final int FAJR = 0, SUNRISE = 1, DHUHR = 2, ASR = 3, MAGHRIB = 4, ISHA = 5;

    private static final double FAJR_ANGLE = 18, ISHA_ANGLE = 17, SUN_ANGLE = 0.833;
    private static final int ASR_FACTOR = 1;   // Diyanet: asr-ı evvel (the Hanafi asr-ı sani would be 2, ~40 min later in winter)
    /** Safety margins in minutes, Diyanet style (earlier sunrise, later noon/afternoon/sunset). Indexed like the constants above. */
    private static final double[] TUNE_MIN = {0, -7, 5, 4, 7, 0};

    /** @return 6 epoch-millisecond instants (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha) for the given local date */
    static long[] times(double lat, double lon, int year, int month, int day, TimeZone tz) {
        long utcMidnight = daysFromCivil(year, month, day) * 86400000L;
        double tzHours = tz.getOffset(utcMidnight + 12L * 3600000L - tz.getRawOffset()) / 3600000.0;   // offset around local noon
        double jd = julian(year, month, day) - lon / (15.0 * 24.0);

        double[] t = {5, 6, 12, 13, 18, 18};
        for (int i = 0; i < t.length; i++) t[i] /= 24.0;
        double[] r = compute(lat, jd, t);
        double[] again = new double[6];                                 // second pass: refine with the first result (hours -> day fractions)
        for (int i = 0; i < 6; i++) again[i] = (Double.isNaN(r[i]) ? t[i] * 24.0 : r[i]) / 24.0;
        r = compute(lat, jd, again);

        double[] h = new double[6];
        for (int i = 0; i < 6; i++) h[i] = r[i] + tzHours - lon / 15.0;
        highLatitudes(h, lat);
        long[] out = new long[6];
        for (int i = 0; i < 6; i++) {
            double hours = h[i] + TUNE_MIN[i] / 60.0;
            out[i] = utcMidnight + Math.round((hours - tzHours) * 3600000.0);
        }
        return out;
    }

    // ---- solar math -----------------------------------------------------------------------------

    private static double[] compute(double lat, double jd, double[] t) {
        double[] o = new double[6];
        o[FAJR] = angleTime(lat, jd, FAJR_ANGLE, t[FAJR], true);
        o[SUNRISE] = angleTime(lat, jd, SUN_ANGLE, t[SUNRISE], true);
        o[DHUHR] = midDay(jd, t[DHUHR]);
        o[ASR] = asrTime(lat, jd, ASR_FACTOR, t[ASR]);
        o[MAGHRIB] = angleTime(lat, jd, SUN_ANGLE, t[MAGHRIB], false);
        o[ISHA] = angleTime(lat, jd, ISHA_ANGLE, t[ISHA], false);
        return o;
    }

    /** Equation of time (hours) and declination (degrees) at Julian date jd. */
    private static double[] sun(double jd) {
        double d = jd - 2451545.0;
        double g = fix(357.529 + 0.98560028 * d, 360);
        double q = fix(280.459 + 0.98564736 * d, 360);
        double L = fix(q + 1.915 * sin(g) + 0.020 * sin(2 * g), 360);
        double e = 23.439 - 0.00000036 * d;
        double ra = Math.toDegrees(Math.atan2(cos(e) * sin(L), cos(L))) / 15.0;
        double eqt = q / 15.0 - fix(ra, 24);
        double decl = Math.toDegrees(Math.asin(sin(e) * sin(L)));
        return new double[]{eqt, decl};
    }

    private static double midDay(double jd, double t) { return fix(12 - sun(jd + t)[0], 24); }

    private static double angleTime(double lat, double jd, double angle, double t, boolean before) {
        double decl = sun(jd + t)[1], noon = midDay(jd, t);
        double x = (-sin(angle) - sin(decl) * sin(lat)) / (cos(decl) * cos(lat));
        double h = Math.toDegrees(Math.acos(x)) / 15.0;     // NaN when the sun never reaches that angle
        return noon + (before ? -h : h);
    }

    private static double asrTime(double lat, double jd, int factor, double t) {
        double decl = sun(jd + t)[1];
        double angle = -Math.toDegrees(Math.atan(1.0 / (factor + Math.tan(Math.toRadians(Math.abs(lat - decl))))));
        return angleTime(lat, jd, angle, t, false);
    }

    /** Fajr/Isha when the twilight angle is not reached or lies unreasonably far from sunrise/sunset: angle-based night portion. */
    private static void highLatitudes(double[] h, double lat) {
        double night = fix(h[SUNRISE] - h[MAGHRIB], 24);
        double fajrLimit = night * (FAJR_ANGLE / 60.0), ishaLimit = night * (ISHA_ANGLE / 60.0);
        if (Double.isNaN(h[FAJR]) || fix(h[SUNRISE] - h[FAJR], 24) > fajrLimit) h[FAJR] = h[SUNRISE] - fajrLimit;
        if (Double.isNaN(h[ISHA]) || fix(h[ISHA] - h[MAGHRIB], 24) > ishaLimit) h[ISHA] = h[MAGHRIB] + ishaLimit;
    }

    // ---- helpers ----------------------------------------------------------------------------------

    private static double sin(double d) { return Math.sin(Math.toRadians(d)); }
    private static double cos(double d) { return Math.cos(Math.toRadians(d)); }
    private static double fix(double a, double m) { a = a - m * Math.floor(a / m); return a < 0 ? a + m : a; }

    private static double julian(int y, int m, int d) {
        if (m <= 2) { y -= 1; m += 12; }
        double a = Math.floor(y / 100.0), b = 2 - a + Math.floor(a / 4.0);
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524.5;
    }

    /** Days since 1970-01-01 of a proleptic Gregorian date. */
    private static long daysFromCivil(int y, int m, int d) {
        y -= m <= 2 ? 1 : 0;
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153L * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }
}
