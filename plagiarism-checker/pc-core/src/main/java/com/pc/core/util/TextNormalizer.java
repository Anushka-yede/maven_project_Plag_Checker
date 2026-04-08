package com.pc.core.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Normalizes raw document text for consistent similarity comparison.
 *
 * <p>Pipeline:
 * <ol>
 *   <li>Unicode NFC normalization</li>
 *   <li>Lowercase</li>
 *   <li>Strip punctuation and special characters</li>
 *   <li>Collapse whitespace</li>
 *   <li>Porter stem each token</li>
 * </ol>
 */
public final class TextNormalizer {

    private static final Pattern NON_ALPHA = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    // Prevent instantiation
    private TextNormalizer() {}

    /**
     * Full normalization pipeline — returns stemmed, lowercased, punctuation-free text.
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return "";

        // 1. Unicode normalize
        String text = Normalizer.normalize(raw, Normalizer.Form.NFC);

        // 2. Lowercase
        text = text.toLowerCase();

        // 3. Remove punctuation
        text = NON_ALPHA.matcher(text).replaceAll(" ");

        // 4. Collapse whitespace
        text = MULTI_SPACE.matcher(text).replaceAll(" ").strip();

        // 5. Porter stem each token
        String[] tokens = text.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (!token.isBlank()) {
                sb.append(porterStem(token)).append(' ');
            }
        }
        return sb.toString().strip();
    }

    /**
     * Lightweight Porter Stemmer (Phase 1–5 rules).
     * Covers the most common English suffixes without an external library.
     */
    public static String porterStem(String word) {
        if (word.length() <= 2) return word;
        word = step1a(word);
        word = step1b(word);
        word = step1c(word);
        word = step2(word);
        word = step3(word);
        word = step4(word);
        word = step5a(word);
        return word;
    }

    // ──────────────── Porter Steps ────────────────

    private static String step1a(String w) {
        if (w.endsWith("sses")) return w.substring(0, w.length() - 2);
        if (w.endsWith("ies"))  return w.substring(0, w.length() - 2);
        if (w.endsWith("ss"))   return w;
        if (w.endsWith("s"))    return w.substring(0, w.length() - 1);
        return w;
    }

    private static String step1b(String w) {
        if (w.endsWith("eed")) {
            String stem = w.substring(0, w.length() - 3);
            return measure(stem) > 0 ? stem + "ee" : w;
        }
        if (w.endsWith("ed")) {
            String stem = w.substring(0, w.length() - 2);
            if (containsVowel(stem)) return cleanEnding(stem);
        }
        if (w.endsWith("ing")) {
            String stem = w.substring(0, w.length() - 3);
            if (containsVowel(stem)) return cleanEnding(stem);
        }
        return w;
    }

    private static String cleanEnding(String stem) {
        if (stem.endsWith("at") || stem.endsWith("bl") || stem.endsWith("iz")) return stem + "e";
        if (isDoubleConsonant(stem) && !stem.endsWith("l") && !stem.endsWith("s") && !stem.endsWith("z"))
            return stem.substring(0, stem.length() - 1);
        if (measure(stem) == 1 && cvcPattern(stem)) return stem + "e";
        return stem;
    }

    private static String step1c(String w) {
        if (w.endsWith("y") && containsVowel(w.substring(0, w.length() - 1)))
            return w.substring(0, w.length() - 1) + "i";
        return w;
    }

    private static String step2(String w) {
        record R(String suf, String rep) {}
        List<R> rules = List.of(
            new R("ational","ate"), new R("tional","tion"),
            new R("enci","ence"), new R("anci","ance"),
            new R("izer","ize"), new R("iser","ise"),
            new R("alism","al"), new R("ation","ate"),
            new R("aliti","al"), new R("ousli","ous"),
            new R("ousness","ous"), new R("iveness","ive"),
            new R("fulness","ful"), new R("entli","ent"),
            new R("eli","e"), new R("biliti","ble")
        );
        for (R r : rules) {
            if (w.endsWith(r.suf())) {
                String stem = w.substring(0, w.length() - r.suf().length());
                if (measure(stem) > 0) return stem + r.rep();
            }
        }
        return w;
    }

    private static String step3(String w) {
        record R(String suf, String rep) {}
        List<R> rules = List.of(
            new R("icate","ic"), new R("ative",""), new R("alize","al"),
            new R("iciti","ic"), new R("ical","ic"), new R("ful",""), new R("ness","")
        );
        for (R r : rules) {
            if (w.endsWith(r.suf())) {
                String stem = w.substring(0, w.length() - r.suf().length());
                if (measure(stem) > 0) return stem + r.rep();
            }
        }
        return w;
    }

    private static String step4(String w) {
        String[] suffixes = {"al","ance","ence","er","ic","able","ible","ant","ement",
                             "ment","ent","ion","ou","ism","ate","iti","ous","ive","ize"};
        for (String suf : suffixes) {
            if (w.endsWith(suf)) {
                String stem = w.substring(0, w.length() - suf.length());
                if (suf.equals("ion") && (stem.endsWith("s") || stem.endsWith("t"))) {
                    if (measure(stem) > 1) return stem;
                } else if (measure(stem) > 1) {
                    return stem;
                }
            }
        }
        return w;
    }

    private static String step5a(String w) {
        if (w.endsWith("e")) {
            String stem = w.substring(0, w.length() - 1);
            if (measure(stem) > 1) return stem;
            if (measure(stem) == 1 && !cvcPattern(stem)) return stem;
        }
        return w;
    }

    // ──────────────── Helper Methods ────────────────

    private static boolean isVowel(char c) {
        return "aeiou".indexOf(c) >= 0;
    }

    private static boolean containsVowel(String w) {
        for (char c : w.toCharArray()) if (isVowel(c)) return true;
        return false;
    }

    private static boolean isDoubleConsonant(String w) {
        if (w.length() < 2) return false;
        char last = w.charAt(w.length() - 1);
        char prev = w.charAt(w.length() - 2);
        return last == prev && !isVowel(last);
    }

    private static boolean cvcPattern(String w) {
        if (w.length() < 3) return false;
        char c1 = w.charAt(w.length() - 3);
        char c2 = w.charAt(w.length() - 2);
        char c3 = w.charAt(w.length() - 1);
        return !isVowel(c1) && isVowel(c2) && !isVowel(c3)
               && c3 != 'w' && c3 != 'x' && c3 != 'y';
    }

    /**
     * Counts the number of VC sequences (measure) in a word stem.
     */
    static int measure(String stem) {
        int m = 0;
        boolean prevVowel = false;
        for (char c : stem.toCharArray()) {
            boolean currVowel = isVowel(c);
            if (!currVowel && prevVowel) m++;
            prevVowel = currVowel;
        }
        return m;
    }
}
