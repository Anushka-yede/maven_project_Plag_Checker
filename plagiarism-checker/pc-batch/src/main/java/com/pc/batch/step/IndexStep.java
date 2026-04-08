package com.pc.batch.step;

import com.pc.core.util.TextNormalizer;
import com.pc.engine.indexer.LuceneIndexWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Step 2 — Normalize and index document text into Lucene.
 *
 * <p>Reads {@code rawText} and {@code submissionId} from execution context.
 * Normalizes via {@link TextNormalizer} and writes TF-IDF term vectors
 * into the Lucene FSDirectory index. Writes {@code normalizedText} back.
 */
@Component
public class IndexStep implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(IndexStep.class);

    @Value("${lucene.index-dir:./lucene-index}")
    private String indexDir;

    @Override
    public RepeatStatus execute(StepContribution contribution,
                                ChunkContext chunkContext) throws Exception {
        var ctx = chunkContext.getStepContext().getStepExecution()
                              .getJobExecution().getExecutionContext();

        String rawText      = (String) ctx.get("rawText");
        String submissionId = (String) ctx.get("submissionId");

        if (rawText == null || rawText.isBlank()) {
            throw new IllegalStateException("No rawText in context");
        }

        String normalized = TextNormalizer.normalize(rawText);
        ctx.putString("normalizedText", normalized);

        log.info("IndexStep: indexing submission {} ({} tokens)",
                submissionId, normalized.split("\\s+").length);

        try (LuceneIndexWriter writer = new LuceneIndexWriter(Path.of(indexDir))) {
            writer.index(submissionId, rawText);
        }

        log.info("IndexStep: indexed submission {} successfully", submissionId);
        contribution.incrementWriteCount(1);
        return RepeatStatus.FINISHED;
    }
}
