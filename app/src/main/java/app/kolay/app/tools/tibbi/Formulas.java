package app.kolay.app.tools.tibbi;

/** Clinical formulas. Pure Java (no Android) so they can be checked against published examples on a desktop JVM. */
public final class Formulas {
    private Formulas() {}

    public static final double CREA_UMOL_PER_MGDL = 88.42;

    /** CKD-EPI 2021 (race-free) eGFR in mL/min/1.73 m²; creatinine in mg/dL. */
    public static double egfr2021(double creaMgDl, double age, boolean female) {
        double k = female ? 0.7 : 0.9, a = female ? -0.241 : -0.302, r = creaMgDl / k;
        return 142 * Math.pow(Math.min(r, 1), a) * Math.pow(Math.max(r, 1), -1.200) * Math.pow(0.9938, age) * (female ? 1.012 : 1);
    }

    /** Cockcroft–Gault creatinine clearance in mL/min; weight in kg, creatinine in mg/dL. */
    public static double cockcroftGault(double creaMgDl, double age, double weightKg, boolean female) {
        return (140 - age) * weightKg / (72 * creaMgDl) * (female ? 0.85 : 1);
    }

    public static double bmi(double heightCm, double weightKg) { double m = heightCm / 100; return weightKg / (m * m); }

    /** Mosteller body surface area in m². */
    public static double bsaMosteller(double heightCm, double weightKg) { return Math.sqrt(heightCm * weightKg / 3600); }

    private static double rr(double hr) { return 60.0 / hr; }
    public static double qtcBazett(double qtMs, double hr) { return qtMs / Math.sqrt(rr(hr)); }
    public static double qtcFridericia(double qtMs, double hr) { return qtMs / Math.cbrt(rr(hr)); }
    public static double qtcFramingham(double qtMs, double hr) { return qtMs + 154 * (1 - rr(hr)); }

    /** Payne: albumin-corrected calcium. mg/dL with albumin g/dL, or mmol/L with albumin g/L. */
    public static double correctedCaMgDl(double ca, double albGdl) { return ca + 0.8 * (4 - albGdl); }
    public static double correctedCaMmol(double ca, double albGl) { return ca + 0.02 * (40 - albGl); }

    public static double anionGap(double na, double cl, double hco3) { return na - (cl + hco3); }
    /** Figge: +2.5 mmol/L for each g/dL of albumin below 4. */
    public static double anionGapCorrected(double ag, double albGdl) { return ag + 2.5 * (4 - albGdl); }
}
