package app.kolay.app.tools.tibbi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** A calculator: input fields plus a function from their values to a result. Pure Java; the screen renders it. */
final class Calc {
    enum Kind { NUM, CHECK, CHOICE }

    static final class Field {
        final Kind kind; final String key, label;
        String unit, hint; boolean optional;
        double points;                        // CHECK: points when on
        String[] options; double[] values;    // CHOICE
        int initial;
        Field(Kind kind, String key, String label) { this.kind = kind; this.key = key; this.label = label; }
    }

    /** Field values: numbers as typed (NaN when empty/invalid), checks as their points (0 off), choices as the picked value. */
    static final class V {
        final Map<String, Double> m = new HashMap<>();
        double get(String k) { Double d = m.get(k); return d == null ? Double.NaN : d; }
        boolean has(String k) { return !Double.isNaN(get(k)); }
        boolean on(String k) { return get(k) != 0; }
        int i(String k) { return (int) Math.round(get(k)); }
        double sum(String... keys) { double s = 0; for (String k : keys) s += get(k); return s; }
    }

    /** level: 0 neutral, 1 good, 2 caution, 3 serious. A result with only {@code error} set means "fix the input". */
    static final class Result {
        String value, unit, verdict, detail, error; int level;
        static Result of(String value, String unit, int level, String verdict, String detail) {
            Result r = new Result(); r.value = value; r.unit = unit; r.level = level; r.verdict = verdict; r.detail = detail; return r;
        }
        static Result err(String msg) { Result r = new Result(); r.error = msg; return r; }
    }

    interface Fn { /** @return null while required inputs are missing */ Result run(V v); }

    final String group, title, sub, note;
    final List<Field> fields = new ArrayList<>();
    Fn fn;

    Calc(String group, String title, String sub, String note) { this.group = group; this.title = title; this.sub = sub; this.note = note; }

    Calc num(String key, String label, String unit, String hint) { Field f = new Field(Kind.NUM, key, label); f.unit = unit; f.hint = hint; fields.add(f); return this; }
    Calc opt(String key, String label, String unit, String hint) { num(key, label, unit, hint); fields.get(fields.size() - 1).optional = true; return this; }
    Calc check(String key, String label, double points) { Field f = new Field(Kind.CHECK, key, label); f.points = points; fields.add(f); return this; }
    Calc choice(String key, String label, String[] options, double[] values, int initial) {
        Field f = new Field(Kind.CHOICE, key, label); f.options = options; f.values = values; f.initial = initial; fields.add(f); return this;
    }
    Calc fn(Fn fn) { this.fn = fn; return this; }

    /** Values with every field at its initial state (checks off, choices at their default, numbers empty). */
    V blank() {
        V v = new V();
        for (Field f : fields) v.m.put(f.key, f.kind == Kind.NUM ? Double.NaN : f.kind == Kind.CHECK ? 0 : f.values[f.initial]);
        return v;
    }

    /** "1,5" / "1.5" → 1.5; anything else → NaN. */
    static double parse(String s) {
        s = s == null ? "" : s.trim().replace(',', '.');
        if (s.isEmpty()) return Double.NaN;
        try { double d = Double.parseDouble(s); return Double.isFinite(d) ? d : Double.NaN; } catch (NumberFormatException e) { return Double.NaN; }
    }

    /** Turkish decimal comma, {@code dec} decimals. */
    static String n(double v, int dec) { return String.format(Locale.ROOT, "%." + dec + "f", v).replace('.', ','); }
    /** Score with a half point shown only when present (Wells). */
    static String score(double v) { return v == Math.rint(v) ? String.valueOf((long) v) : n(v, 1); }
}
