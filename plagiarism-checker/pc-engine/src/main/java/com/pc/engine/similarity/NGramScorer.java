package com.pc.engine.similarity;

import java.util.HashSet;
import java.util.Set;

public class NGramScorer {
    
    private static final int N_GRAM_SIZE = 3; // 3-word n-grams (trigrams)

    /**
     * Calculates the Jaccard Similarity of N-Grams between two texts.
     * Score = (Intersection of N-Grams) / (Union of N-Grams)
     * 
     * @param textA Cleaned text A
     * @param textB Cleaned text B
     * @return similarity score between 0.0 and 1.0
     */
    public double calculateSimilarity(String textA, String textB) {
        if (textA == null || textB == null || textA.isBlank() || textB.isBlank()) return 0.0;

        Set<String> ngramsA = getNGrams(textA, N_GRAM_SIZE);
        Set<String> ngramsB = getNGrams(textB, N_GRAM_SIZE);

        if (ngramsA.isEmpty() || ngramsB.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(ngramsA);
        intersection.retainAll(ngramsB);

        Set<String> union = new HashSet<>(ngramsA);
        union.addAll(ngramsB);

        return (double) intersection.size() / union.size(); 
    }

    private Set<String> getNGrams(String text, int n) {
        Set<String> ngrams = new HashSet<>();
        String[] words = text.split("\\s+");
        if (words.length < n) {
            // Document too short, add the whole thing as one n-gram
            ngrams.add(text);
            return ngrams;
        }
        for (int i = 0; i <= words.length - n; i++) {
            StringBuilder ngram = new StringBuilder();
            for (int j = 0; j < n; j++) {
                ngram.append(words[i + j]).append(j < n - 1 ? " " : "");
            }
            ngrams.add(ngram.toString());
        }
        return ngrams;
    }
}
