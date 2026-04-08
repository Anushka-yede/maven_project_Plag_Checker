package com.pc.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for generating n-grams from tokenized text.
 * Used for SimHash fingerprinting and web-check 5-gram extraction.
 */
public final class TokenUtils {

    private TokenUtils() {}

    /**
     * Splits normalized text into individual word tokens.
     */
    public static String[] tokenize(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) return new String[0];
        return normalizedText.trim().split("\\s+");
    }

    /**
     * Generates character n-grams of the given size from a single token.
     */
    public static List<String> charNgrams(String token, int n) {
        List<String> ngrams = new ArrayList<>();
        if (token == null || token.length() < n) return ngrams;
        for (int i = 0; i <= token.length() - n; i++) {
            ngrams.add(token.substring(i, i + n));
        }
        return ngrams;
    }

    /**
     * Generates word-level n-grams (shingles) from a token array.
     *
     * @param tokens  array of word tokens
     * @param n       shingle size (e.g., 3 for trigrams, 5 for 5-grams)
     * @return        list of space-joined n-gram strings
     */
    public static List<String> wordNgrams(String[] tokens, int n) {
        List<String> ngrams = new ArrayList<>();
        if (tokens == null || tokens.length < n) return ngrams;
        for (int i = 0; i <= tokens.length - n; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = i; j < i + n; j++) {
                if (j > i) sb.append(' ');
                sb.append(tokens[j]);
            }
            ngrams.add(sb.toString());
        }
        return ngrams;
    }

    /**
     * Convenience: extract word n-grams directly from normalized text.
     */
    public static List<String> wordNgrams(String normalizedText, int n) {
        return wordNgrams(tokenize(normalizedText), n);
    }

    /**
     * Extracts top-N most-frequent n-grams for web-check queries.
     * Returns at most {@code topK} n-grams, ordered by frequency descending.
     */
    public static List<String> topNgrams(String normalizedText, int n, int topK) {
        List<String> all = wordNgrams(normalizedText, n);
        return all.stream()
                .distinct()
                .limit(topK)
                .toList();
    }
}
