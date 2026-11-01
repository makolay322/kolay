package app.kolay.app.tools.friday;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Hadiths for the Friday card, read once from {@code assets/hadiths.tsv}: one hadith per line, "text<TAB>source".
 *
 * Source: the Turkish Kütüb-i Sitte translation by Prof. Dr. İbrahim Canan as published in the muhaddis.org database
 * (https://www.muhaddis.org/ks.zip). The text is taken verbatim apart from obvious typo fixes and «» for inner quotes;
 * the source line is the first of Buhârî, Müslim, Tirmizî, Ebû Dâvûd, Nesâî, Muvatta that the entry cites, with its
 * book and chapter number exactly as the database gives it. Rezin additions are left out, near-duplicates are removed
 * (the same hadith reported in several collections appears once), and topics that do not suit a greeting card
 * (penal law, inheritance, slavery and the like) were filtered out. Only hadiths that carry advice for the reader
 * remain: prophecies, descriptions of the hereafter, praise of companions, narrations of practice, bare supplication
 * texts and technical rulings were removed.
 */
final class Hadiths {
    private Hadiths() {}

    private static List<String[]> all;

    static synchronized List<String[]> all(Context ctx) {
        if (all != null) return all;
        List<String[]> out = new ArrayList<>(400);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(ctx.getAssets().open("hadiths.tsv"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                int tab = line.indexOf('\t');
                if (tab > 0 && tab < line.length() - 1) out.add(new String[]{line.substring(0, tab), line.substring(tab + 1)});
            }
        } catch (Exception ignored) { }
        if (out.isEmpty()) out.add(new String[]{"Ameller niyetlere göredir.", "Buhârî, Bed'ü'l-Vahy 1"});
        return all = out;
    }
}
