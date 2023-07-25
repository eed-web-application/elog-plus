package edu.stanford.slac.elog_plus.utility;

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
}
