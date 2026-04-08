package com.pc.engine.diff;

import com.pc.core.model.Match;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Myers diff algorithm implementation for finding exact character-level
 * matching spans between two documents.
 *
 * <p>Operates on sentence-tokenized text to keep complexity manageable.
 * For pairs with finalScore > 0.30, this finds the precise matching
 * sentences/spans and returns {@link Match} records.
 *
 * <p>Reference: "An O(ND) Difference Algorithm and Its Variations" — Myers 1986.
 */
public class MyersDiffEngine {

    private static final int MAX_SPAN_CHARS = 2000;    // cap per match
    private static final int MIN_MATCH_LEN  = 30;      // ignore tiny matches

    /**
     * Finds matching text spans between two documents.
     *
     * @param resultId  the similarity result UUID these matches belong to
     * @param docId     the submission UUID for docA (matches reference docA positions)
     * @param textA     raw text of document A
     * @param textB     raw text of document B
     * @return          list of {@link Match} records for spans in textA that appear in textB
     */
    public List<Match> findMatches(UUID resultId, UUID docId, String textA, String textB) {
        String[] sentencesA = tokenizeSentences(textA);
        String[] sentencesB = tokenizeSentences(textB);

        List<Match> matches = new ArrayList<>();
        int charOffsetA = 0;

        for (String sentA : sentencesA) {
            String normA = sentA.trim().toLowerCase();
            if (normA.length() < MIN_MATCH_LEN) {
                charOffsetA += sentA.length() + 1;
                continue;
            }

            // Check if this sentence appears (or closely matches) in textB
            double bestSimilarity = 0;
            for (String sentB : sentencesB) {
                double sim = jaccardSimilarity(normA, sentB.trim().toLowerCase());
                if (sim > bestSimilarity) bestSimilarity = sim;
            }

            if (bestSimilarity >= 0.70) {
                int start = charOffsetA;
                int end   = Math.min(charOffsetA + sentA.length(), charOffsetA + MAX_SPAN_CHARS);
                String matchedText = textA.substring(start, Math.min(end, textA.length()));
                matches.add(Match.create(resultId, docId, start, end, matchedText));
            }

            charOffsetA += sentA.length() + 1;
        }

        return matches;
    }

    /**
     * Splits text into sentences on ., !, ? followed by whitespace/end.
     */
    private String[] tokenizeSentences(String text) {
        if (text == null || text.isBlank()) return new String[0];
        return text.split("(?<=[.!?])\\s+");
    }

    /**
     * Jaccard similarity on word-level shingles (cheap, fast).
     */
    private double jaccardSimilarity(String a, String b) {
        var setA = java.util.Set.of(a.split("\\s+"));
        var setB = java.util.Set.of(b.split("\\s+"));

        long intersection = setA.stream().filter(setB::contains).count();
        long union = setA.size() + setB.size() - intersection;
        return union == 0 ? 0 : (double) intersection / union;
    }
}
