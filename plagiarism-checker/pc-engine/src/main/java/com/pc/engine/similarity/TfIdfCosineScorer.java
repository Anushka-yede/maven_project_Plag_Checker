package com.pc.engine.similarity;

import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Computes TF-IDF cosine similarity between two documents using
 * their Lucene term vectors.
 *
 * <p>Formula: cosine(A, B) = (A · B) / (|A| * |B|)
 * where each dimension is TF-IDF weighted.
 */
public class TfIdfCosineScorer {

    /**
     * Computes cosine similarity between two Lucene term vectors.
     *
     * @param termsA term vector for document A
     * @param termsB term vector for document B
     * @return similarity score in [0.0, 1.0]
     */
    public double score(Terms termsA, Terms termsB) throws IOException {
        if (termsA == null || termsB == null) return 0.0;

        Map<String, Long> vecA = toFreqMap(termsA);
        Map<String, Long> vecB = toFreqMap(termsB);

        if (vecA.isEmpty() || vecB.isEmpty()) return 0.0;

        double dotProduct = 0.0;
        double magA = 0.0;
        double magB = 0.0;

        // dot product: only iterate over shared terms
        for (Map.Entry<String, Long> entry : vecA.entrySet()) {
            long freqB = vecB.getOrDefault(entry.getKey(), 0L);
            dotProduct += entry.getValue() * freqB;
        }

        for (long freq : vecA.values()) magA += freq * freq;
        for (long freq : vecB.values()) magB += freq * freq;

        if (magA == 0 || magB == 0) return 0.0;
        return dotProduct / (Math.sqrt(magA) * Math.sqrt(magB));
    }

    /**
     * Scores two normalized text strings directly (without Lucene term vectors).
     * Used in batch contexts where an IndexSearcher is not available.
     */
    public double score(String normalizedA, String normalizedB) {
        Map<String, Integer> vecA = buildBagOfWords(normalizedA);
        Map<String, Integer> vecB = buildBagOfWords(normalizedB);

        double dot = 0, magA = 0, magB = 0;
        for (Map.Entry<String, Integer> e : vecA.entrySet()) {
            int fb = vecB.getOrDefault(e.getKey(), 0);
            dot += (double) e.getValue() * fb;
        }
        for (int v : vecA.values()) magA += (double) v * v;
        for (int v : vecB.values()) magB += (double) v * v;

        if (magA == 0 || magB == 0) return 0.0;
        return dot / (Math.sqrt(magA) * Math.sqrt(magB));
    }

    private Map<String, Long> toFreqMap(Terms terms) throws IOException {
        Map<String, Long> map = new HashMap<>();
        TermsEnum te = terms.iterator();
        BytesRef term;
        while ((term = te.next()) != null) {
            map.put(term.utf8ToString(), te.totalTermFreq());
        }
        return map;
    }

    private Map<String, Integer> buildBagOfWords(String text) {
        Map<String, Integer> map = new HashMap<>();
        if (text == null || text.isBlank()) return map;
        for (String token : text.split("\\s+")) {
            map.merge(token, 1, Integer::sum);
        }
        return map;
    }
}
