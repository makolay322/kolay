package app.kolay.app.tools.tibbi;

import static app.kolay.app.tools.tibbi.Calc.Result;
import static app.kolay.app.tools.tibbi.Calc.n;
import static app.kolay.app.tools.tibbi.Calc.score;

/** The calculator catalogue, in display order. Pure Java. */
final class Calcs {
    private Calcs() {}

    static final String[] SEX = {"Kadın", "Erkek"};
    static final double[] SEX_V = {1, 0};                       // 1 = female
    static final String[] CREA_UNIT = {"mg/dL", "µmol/L"};
    static final double[] CREA_UNIT_V = {0, 1};

    static final Calc[] ALL = {
        new Calc("BÖBREK", "eGFR (CKD-EPI 2021)", "Kreatinine göre tahmini GFR",
            "Irk katsayısı olmayan 2021 denklemi; 18 yaş ve üstü için. Akut böbrek hasarında, aşırı kas kütlesinde veya kaşekside güvenilmez.")
            .num("crea", "Kreatinin", null, "örn. 1,1").choice("cu", "Birim", CREA_UNIT, CREA_UNIT_V, 0)
            .num("age", "Yaş", "yıl", "örn. 64").choice("sex", "Cinsiyet", SEX, SEX_V, 1)
            .fn(v -> {
                if (!v.has("crea") || !v.has("age")) return null;
                double cr = crea(v);
                if (cr < 0.1 || cr > 25) return Result.err("Kreatinin aralık dışı (birimi kontrol et)");
                if (v.get("age") < 18 || v.get("age") > 120) return Result.err("Yaş 18–120 olmalı");
                double g = Formulas.egfr2021(cr, v.get("age"), v.on("sex"));
                String st = g >= 90 ? "G1 · normal veya yüksek" : g >= 60 ? "G2 · hafif azalmış" : g >= 45 ? "G3a · hafif–orta azalmış"
                    : g >= 30 ? "G3b · orta–ağır azalmış" : g >= 15 ? "G4 · ağır azalmış" : "G5 · böbrek yetmezliği";
                return Result.of(String.valueOf(Math.round(g)), "mL/dk/1,73 m²", g >= 60 ? 1 : g >= 30 ? 2 : 3, st,
                    v.i("cu") == 1 ? "Kreatinin " + n(cr, 2) + " mg/dL olarak hesaplandı" : null);
            }),

        new Calc("BÖBREK", "Kreatinin klirensi", "Cockcroft–Gault",
            "Gerçek vücut ağırlığıyla hesaplanır; obezde ideal veya ayarlanmış ağırlığı düşün. İlaç dozlamasında çoğu prospektüs bu değeri kullanır.")
            .num("crea", "Kreatinin", null, "örn. 1,1").choice("cu", "Birim", CREA_UNIT, CREA_UNIT_V, 0)
            .num("age", "Yaş", "yıl", "örn. 78").num("wt", "Kilo", "kg", "örn. 70").choice("sex", "Cinsiyet", SEX, SEX_V, 1)
            .fn(v -> {
                if (!v.has("crea") || !v.has("age") || !v.has("wt")) return null;
                double cr = crea(v);
                if (cr < 0.1 || cr > 25) return Result.err("Kreatinin aralık dışı (birimi kontrol et)");
                if (v.get("age") < 18 || v.get("age") > 120) return Result.err("Yaş 18–120 olmalı");
                if (v.get("wt") < 25 || v.get("wt") > 350) return Result.err("Kilo 25–350 kg olmalı");
                double cl = Formulas.cockcroftGault(cr, v.get("age"), v.get("wt"), v.on("sex"));
                String st = cl > 60 ? "Normal veya hafif azalmış" : cl >= 30 ? "Orta derecede azalmış" : cl >= 15 ? "Ağır azalmış · doz ayarı/kontrendikasyon" : "Böbrek yetmezliği";
                return Result.of(String.valueOf(Math.round(cl)), "mL/dk", cl > 60 ? 1 : cl >= 30 ? 2 : 3, st, null);
            }),

        new Calc("GENEL", "VKİ ve vücut yüzey alanı", "Boy ve kilodan",
            "VKİ sınıfları DSÖ'ye göre. Vücut yüzey alanı Mosteller formülüyle.")
            .num("h", "Boy", "cm", "örn. 175").num("wt", "Kilo", "kg", "örn. 80")
            .fn(v -> {
                if (!v.has("h") || !v.has("wt")) return null;
                if (v.get("h") < 50 || v.get("h") > 250) return Result.err("Boy 50–250 cm olmalı");
                if (v.get("wt") < 2 || v.get("wt") > 350) return Result.err("Kilo 2–350 kg olmalı");
                double b = Formulas.bmi(v.get("h"), v.get("wt"));
                String st = b < 18.5 ? "Zayıf" : b < 25 ? "Normal" : b < 30 ? "Fazla kilolu" : b < 35 ? "Obez · sınıf I" : b < 40 ? "Obez · sınıf II" : "Obez · sınıf III";
                return Result.of(n(b, 1), "kg/m²", b < 18.5 ? 2 : b < 25 ? 1 : b < 35 ? 2 : 3, st,
                    "Vücut yüzey alanı " + n(Formulas.bsaMosteller(v.get("h"), v.get("wt")), 2) + " m²");
            }),

        new Calc("KARDİYOLOJİ", "QTc", "Kalp hızına göre düzeltilmiş QT",
            "Ana değer Bazett; 60–100/dk dışında Fridericia daha güvenilirdir. Dal bloğunda QT uzun ölçülür.")
            .num("qt", "QT", "ms", "örn. 400").num("hr", "Kalp hızı", "/dk", "örn. 75").choice("sex", "Cinsiyet", SEX, SEX_V, 1)
            .fn(v -> {
                if (!v.has("qt") || !v.has("hr")) return null;
                if (v.get("qt") < 200 || v.get("qt") > 800) return Result.err("QT 200–800 ms olmalı");
                if (v.get("hr") < 20 || v.get("hr") > 250) return Result.err("Kalp hızı 20–250/dk olmalı");
                double b = Formulas.qtcBazett(v.get("qt"), v.get("hr"));
                double lim = v.on("sex") ? 460 : 450;
                String st = b > 500 ? "Belirgin uzun · torsade riski" : b > lim ? "Uzun" : b < 350 ? "Kısa" : "Normal";
                return Result.of(String.valueOf(Math.round(b)), "ms", b > 500 ? 3 : (b > lim || b < 350) ? 2 : 1, st,
                    "Fridericia " + Math.round(Formulas.qtcFridericia(v.get("qt"), v.get("hr"))) + " ms · Framingham "
                        + Math.round(Formulas.qtcFramingham(v.get("qt"), v.get("hr"))) + " ms");
            }),

        new Calc("KARDİYOLOJİ", "CHA₂DS₂-VA", "AF'de inme riski (ESC 2024)",
            "ESC 2024 cinsiyet puanını çıkardı (CHA₂DS₂-VA). Eski CHA₂DS₂-VASc ayrıca gösterilir. Kanama riskini HAS-BLED ile birlikte değerlendir.")
            .choice("age", "Yaş", new String[]{"< 65", "65–74", "≥ 75"}, new double[]{0, 1, 2}, 0)
            .choice("sex", "Cinsiyet", SEX, SEX_V, 1)
            .check("chf", "Kalp yetersizliği / sol ventrikül disfonksiyonu", 1)
            .check("htn", "Hipertansiyon", 1)
            .check("dm", "Diyabet", 1)
            .check("stroke", "İnme, TİA veya tromboemboli öyküsü", 2)
            .check("vasc", "Vasküler hastalık (MI, PAH, aort plağı)", 1)
            .fn(v -> {
                double va = v.sum("age", "chf", "htn", "dm", "stroke", "vasc"), vasc = va + (v.on("sex") ? 1 : 0);
                String st = va >= 2 ? "Oral antikoagülan önerilir" : va == 1 ? "Oral antikoagülan düşünülmeli" : "Antikoagülan gerekmez";
                return Result.of(score(va), "puan", va >= 2 ? 3 : va == 1 ? 2 : 1, st, "CHA₂DS₂-VASc " + score(vasc));
            }),

        new Calc("KARDİYOLOJİ", "HAS-BLED", "Antikoagülasyonda kanama riski",
            "Yüksek skor antikoagülanı kesme nedeni değildir; düzeltilebilir etkenleri (tansiyon, alkol, NSAİİ) ele almak ve yakın izlem içindir.")
            .check("h", "Kontrolsüz hipertansiyon (sistolik > 160)", 1)
            .check("r", "Böbrek fonksiyon bozukluğu (diyaliz, Tx, Cr > 2,26 mg/dL)", 1)
            .check("l", "Karaciğer hastalığı (siroz, bilirubin > 2×, AST/ALT > 3×)", 1)
            .check("s", "İnme öyküsü", 1)
            .check("b", "Kanama öyküsü veya yatkınlığı", 1)
            .check("inr", "Değişken INR (TTR < %60)", 1)
            .check("e", "Yaş > 65", 1)
            .check("d", "Antiplatelet veya NSAİİ kullanımı", 1)
            .check("a", "Alkol (≥ 8 kadeh/hafta)", 1)
            .fn(v -> {
                double s = v.sum("h", "r", "l", "s", "b", "inr", "e", "d", "a");
                return Result.of(score(s), "puan", s >= 3 ? 3 : s == 2 ? 2 : 1,
                    s >= 3 ? "Yüksek kanama riski" : s == 2 ? "Orta kanama riski" : "Düşük kanama riski", null);
            }),

        new Calc("ACİL", "Wells (pulmoner emboli)", "PE klinik olasılığı",
            "İki kademeli yorum. PE olası değilse ve PERC/D-dimer negatifse ileri görüntüleme genellikle gerekmez.")
            .check("dvt", "DVT klinik bulguları", 3)
            .check("alt", "PE en olası tanı", 3)
            .check("hr", "Kalp hızı > 100/dk", 1.5)
            .check("imm", "Son 4 haftada immobilizasyon veya cerrahi", 1.5)
            .check("prev", "Önceden DVT veya PE", 1.5)
            .check("hem", "Hemoptizi", 1)
            .check("ca", "Aktif kanser", 1)
            .fn(v -> {
                double s = v.sum("dvt", "alt", "hr", "imm", "prev", "hem", "ca");
                return Result.of(score(s), "puan", s > 4 ? 3 : 1, s > 4 ? "PE olası · BT anjiyografi" : "PE olası değil · D-dimer", null);
            }),

        new Calc("ACİL", "CURB-65", "Toplum kökenli pnömoni ciddiyeti", null)
            .check("c", "Yeni gelişen konfüzyon", 1)
            .check("u", "Üre > 42 mg/dL (> 7 mmol/L; BUN > 19 mg/dL)", 1)
            .check("r", "Solunum sayısı ≥ 30/dk", 1)
            .check("b", "Sistolik < 90 veya diyastolik ≤ 60 mmHg", 1)
            .check("a", "Yaş ≥ 65", 1)
            .fn(v -> {
                double s = v.sum("c", "u", "r", "b", "a");
                return Result.of(score(s), "puan", s >= 3 ? 3 : s == 2 ? 2 : 1,
                    s >= 3 ? "Ağır · yatış, 4–5'te yoğun bakım düşün" : s == 2 ? "Orta · yatarak tedavi düşün" : "Düşük · ayaktan tedavi", null);
            }),

        new Calc("ACİL", "qSOFA", "Sepsis şüphesinde kötü seyir riski",
            "Tanı ölçütü değildir; ≥ 2 puan kötü seyir riskini gösterir, SOFA ile değerlendir.")
            .check("rr", "Solunum sayısı ≥ 22/dk", 1)
            .check("ms", "Bilinç değişikliği (GKS < 15)", 1)
            .check("sbp", "Sistolik ≤ 100 mmHg", 1)
            .fn(v -> {
                double s = v.sum("rr", "ms", "sbp");
                return Result.of(score(s), "puan", s >= 2 ? 3 : 1, s >= 2 ? "Yüksek risk · sepsis açısından değerlendir" : "Düşük risk", null);
            }),

        new Calc("ACİL", "Glasgow Koma Skalası", "Göz, sözel, motor yanıt", "Entübe hastada sözel yanıt değerlendirilemez (V1T olarak yaz).")
            .choice("e", "Göz açma", new String[]{"4 · Kendiliğinden", "3 · Sesle", "2 · Ağrıyla", "1 · Yok"}, new double[]{4, 3, 2, 1}, 0)
            .choice("vb", "Sözel yanıt", new String[]{"5 · Oryante", "4 · Konfüze", "3 · Uygunsuz kelimeler", "2 · Anlamsız sesler", "1 · Yok"},
                new double[]{5, 4, 3, 2, 1}, 0)
            .choice("m", "Motor yanıt", new String[]{"6 · Emirlere uyar", "5 · Ağrıyı lokalize eder", "4 · Ağrıdan çeker", "3 · Anormal fleksiyon",
                "2 · Ekstansiyon", "1 · Yok"}, new double[]{6, 5, 4, 3, 2, 1}, 0)
            .fn(v -> {
                int s = v.i("e") + v.i("vb") + v.i("m");
                return Result.of(String.valueOf(s), "/ 15", s >= 13 ? 1 : s >= 9 ? 2 : 3,
                    s >= 13 ? "Hafif" : s >= 9 ? "Orta" : "Ağır · hava yolu koruması düşün", "E" + v.i("e") + " V" + v.i("vb") + " M" + v.i("m"));
            }),

        new Calc("LABORATUVAR", "Düzeltilmiş kalsiyum", "Albümine göre (Payne)", "İyonize kalsiyum ölçülebiliyorsa o daha güvenilirdir.")
            .num("ca", "Kalsiyum", null, "örn. 8,2").num("alb", "Albümin", null, "örn. 3,0")
            .choice("u", "Birimler", new String[]{"mg/dL · g/dL", "mmol/L · g/L"}, new double[]{0, 1}, 0)
            .fn(v -> {
                if (!v.has("ca") || !v.has("alb")) return null;
                boolean si = v.i("u") == 1;
                double ca = v.get("ca"), alb = v.get("alb");
                if (si ? (ca < 0.5 || ca > 6 || alb < 5 || alb > 70) : (ca < 2 || ca > 25 || alb < 0.5 || alb > 7))
                    return Result.err("Değerler seçilen birimle uyuşmuyor");
                double cc = si ? Formulas.correctedCaMmol(ca, alb) : Formulas.correctedCaMgDl(ca, alb);
                double lo = si ? 2.15 : 8.6, hi = si ? 2.6 : 10.4;
                return Result.of(n(cc, si ? 2 : 1), si ? "mmol/L" : "mg/dL", cc < lo || cc > hi ? 2 : 1,
                    cc < lo ? "Düşük" : cc > hi ? "Yüksek" : "Normal aralıkta", "Normal aralık " + n(lo, si ? 2 : 1) + "–" + n(hi, si ? 2 : 1));
            }),

        new Calc("LABORATUVAR", "Anyon açığı", "Na − (Cl + HCO₃)", "Potasyumsuz hesap; normal değer laboratuvara göre değişir (yaklaşık 8–12).")
            .num("na", "Sodyum", "mmol/L", "örn. 140").num("cl", "Klor", "mmol/L", "örn. 104").num("hco3", "Bikarbonat", "mmol/L", "örn. 24")
            .opt("alb", "Albümin (isteğe bağlı)", "g/dL", "düzeltme için")
            .fn(v -> {
                if (!v.has("na") || !v.has("cl") || !v.has("hco3")) return null;
                if (v.get("na") < 90 || v.get("na") > 200 || v.get("cl") < 50 || v.get("cl") > 160 || v.get("hco3") < 1 || v.get("hco3") > 60)
                    return Result.err("Değerler aralık dışı");
                double ag = Formulas.anionGap(v.get("na"), v.get("cl"), v.get("hco3"));
                boolean alb = v.has("alb") && v.get("alb") > 0.5 && v.get("alb") < 7;
                double use = alb ? Formulas.anionGapCorrected(ag, v.get("alb")) : ag;
                return Result.of(n(use, 1), "mmol/L", use > 12 ? 2 : use < 3 ? 2 : 1, use > 12 ? "Yüksek anyon açığı" : use < 3 ? "Düşük" : "Normal",
                    alb ? "Albüminle düzeltilmiş · düzeltmesiz " + n(ag, 1) : null);
            }),
    };

    /** Creatinine in mg/dL regardless of the chosen unit. */
    private static double crea(Calc.V v) { return v.i("cu") == 1 ? v.get("crea") / Formulas.CREA_UMOL_PER_MGDL : v.get("crea"); }
}
