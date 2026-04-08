package com.pc.engine.similarity;

import com.pc.core.model.SimilarityResult;
import com.pc.core.util.TextNormalizer;
import com.pc.engine.indexer.LuceneIndexSearcher;
import org.apache.lucene.index.Terms;

import java.io.IOException;
import java.util.UUID;

/**
 * Orchestrates all three similarity algorithms and produces a weighted final score.
 *
 * <p>Weights: TF-IDF 40% + SimHash 30% + Semantic 30%
 *
 * <p>Pipeline:
 * <ol>
 *   <li>Normalize both texts</li>
 *   <li>Compute TF-IDF cosine via Lucene term vectors (or bag-of-words fallback)</li>
 *   <li>Compute SimHash Hamming similarity</li>
 *   <li>Compute semantic cosine via averaged GloVe embeddings</li>
 *   <li>Blend and return a {@link SimilarityResult}</li>
 * </ol>
 */
public class SimilarityOrchestrator {

    private final TfIdfCosineScorer   tfidfScorer   = new TfIdfCosineScorer();
    private final SimHashFingerprinter simhash        = new SimHashFingerprinter();
    private final SemanticEmbeddingScorer semantic    = new SemanticEmbeddingScorer();

    /**
     * Computes full similarity between two raw texts.
     *
     * @param docAId  submission UUID for document A
     * @param docBId  submission UUID for document B
     * @param rawA    raw (un-normalized) text of document A
     * @param rawB    raw (un-normalized) text of document B
     * @param aiProb  AI-authorship probability (pre-computed by pc-ai)
     * @return        populated {@link SimilarityResult}
     */
    public SimilarityResult compare(UUID docAId, UUID docBId,
                                    String rawA, String rawB,
                                    double aiProb) {
        String normA = TextNormalizer.normalize(rawA);
        String normB = TextNormalizer.normalize(rawB);

        double tfidf   = tfidfScorer.score(normA, normB);
        double simhashS = simhash.similarity(normA, normB);
        double semanticS = semantic.score(normA, normB);

        return SimilarityResult.create(docAId, docBId, tfidf, simhashS, semanticS, aiProb);
    }

    /**
     * Variant that uses Lucene term vectors when an IndexSearcher is available.
     */
    public SimilarityResult compareWithIndex(UUID docAId, UUID docBId,
                                             String rawA, String rawB,
                                             LuceneIndexSearcher searcher,
                                             double aiProb) throws IOException {
        String normA = TextNormalizer.normalize(rawA);
        String normB = TextNormalizer.normalize(rawB);

        // Lucene term vector cosine
        Terms tvA = searcher.getTermVector(docAId.toString());
        Terms tvB = searcher.getTermVector(docBId.toString());
        double tfidf = (tvA != null && tvB != null)
                ? tfidfScorer.score(tvA, tvB)
                : tfidfScorer.score(normA, normB);

        double simhashS  = simhash.similarity(normA, normB);
        double semanticS = semantic.score(normA, normB);

        return SimilarityResult.create(docAId, docBId, tfidf, simhashS, semanticS, aiProb);
    }

    /** Returns true if the final score exceeds the plagiarism threshold. */
    public boolean exceedsThreshold(SimilarityResult result, double threshold) {
        return result.finalScore() >= threshold;
    }
}
