package com.pc.core.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable domain record representing the similarity comparison result
 * between two documents. Stored in {@code similarity_results}.
 */
public record SimilarityResult(
        UUID id,
        UUID docAId,
        UUID docBId,
        double tfidfScore,
        double simhashScore,
        double semanticScore,
        double aiProbability,       // 0 = human, 1 = AI
        double finalScore,          // weighted blend
        String algorithm,
        Instant matchedAt
) {
    /**
     * Weighted blend: TF-IDF 40% + SimHash 30% + Semantic 30%.
     */
    public static double computeWeightedScore(double tfidf, double simhash, double semantic) {
        return (tfidf * 0.40) + (simhash * 0.30) + (semantic * 0.30);
    }

    public static SimilarityResult create(UUID docAId, UUID docBId,
                                          double tfidf, double simhash, double semantic,
                                          double aiProbability) {
        double finalScore = computeWeightedScore(tfidf, simhash, semantic);
        return new SimilarityResult(
                UUID.randomUUID(),
                docAId,
                docBId,
                tfidf,
                simhash,
                semantic,
                aiProbability,
                finalScore,
                "TFIDF+SIMHASH+SEMANTIC",
                Instant.now()
        );
    }

    /** Risk classification based on final score. */
    public String riskLevel() {
        if (finalScore >= 0.75) return "HIGH";
        if (finalScore >= 0.40) return "MEDIUM";
        return "LOW";
    }
}
