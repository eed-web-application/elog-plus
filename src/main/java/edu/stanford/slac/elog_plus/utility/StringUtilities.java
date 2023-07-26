package edu.stanford.slac.elog_plus.utility;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.text.Normalizer;

public class StringUtilities {
    static public String tagNameNormalization(String tagName) {
        return Normalizer
                .normalize(
                        tagName.trim().toLowerCase(),
                        Normalizer.Form.NFD
                ).replaceAll("[^\\p{ASCII}]", "")
                .replaceAll(" ", "-");
    }

    static public String shiftNameNormalization(String tagName) {
        return Normalizer
                .normalize(
                        tagName.trim(),
                        Normalizer.Form.NFD
                ).replaceAll("[^\\p{ASCII}]", "")
                .replaceAll(" ", "-");
    }

    public static String sanitizeEntryTitle(String title) {
        return Jsoup.clean(title, Safelist.basic());
    }

    public static String sanitizeEntryText(String text) {
        Safelist safelist = Safelist.basic();
        safelist.addTags("h1", "h2", "h3", "h4");
        return Jsoup.clean(text, safelist);
    }
}
