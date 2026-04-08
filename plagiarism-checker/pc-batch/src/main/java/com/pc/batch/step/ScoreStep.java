package com.pc.batch.step;

import com.pc.core.model.Match;
import com.pc.core.model.SimilarityResult;
import com.pc.engine.diff.MyersDiffEngine;
import com.pc.engine.indexer.LuceneIndexSearcher;
import com.pc.engine.similarity.SimilarityOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Step 3 — Compute similarity scores against all indexed documents.
 *
 * <p>Finds top-K similar documents via Lucene search, computes
 * TF-IDF + SimHash + Semantic weighted score, runs Myers diff for
 * matched spans on pairs above the 0.30 threshold.
 *
 * <p>Stores results in execution context for DB persistence in pc-api.
 */
@Component
public class ScoreStep implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(ScoreStep.class);
    private static final double THRESHOLD = 0.30;
    private static final int TOP_K = 10;

    @Value("${lucene.index-dir:./lucene-index}")
    private String indexDir;

    private final SimilarityOrchestrator orchestrator = new SimilarityOrchestrator();
    private final MyersDiffEngine diffEngine = new MyersDiffEngine();

    @Override
    public RepeatStatus execute(StepContribution contribution,
                                ChunkContext chunkContext) throws Exception {
        var ctx = chunkContext.getStepContext().getStepExecution()
                              .getJobExecution().getExecutionContext();

        String normalizedText = (String) ctx.get("normalizedText");
        String rawText        = (String) ctx.get("rawText");
        String submissionId   = (String) ctx.get("submissionId");
        UUID docId = UUID.fromString(submissionId);

        log.info("ScoreStep: scoring submission {}", submissionId);

        List<SimilarityResult> results;
        List<Match> allMatches;

        try (LuceneIndexSearcher searcher = new LuceneIndexSearcher(Path.of(indexDir))) {
            var hits = searcher.search(normalizedText, TOP_K);

            results = hits.stream()
                    .filter(h -> !h.submissionId().equals(submissionId))
                    .map(h -> {
                        try {
                            // ai_probability placeholder — filled by AiAnalysisStep
                            return orchestrator.compareWithIndex(
                                    docId, UUID.fromString(h.submissionId()),
                                    rawText, normalizedText, searcher, 0.0);
                        } catch (Exception e) {
                            log.warn("Score failed for pair {}/{}: {}", submissionId, h.submissionId(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(r -> r != null && r.finalScore() >= THRESHOLD)
                    .toList();

            allMatches = results.stream()
                    .flatMap(r -> diffEngine.findMatches(r.id(), docId, rawText, normalizedText).stream())
                    .toList();
        }

        log.info("ScoreStep: {} pairs above threshold, {} spans found", results.size(), allMatches.size());
        ctx.put("similarityResults", results);
        ctx.put("matchedSpans", allMatches);
        contribution.incrementWriteCount(results.size());
        return RepeatStatus.FINISHED;
    }
}
