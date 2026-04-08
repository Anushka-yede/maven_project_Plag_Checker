package com.pc.batch.step;

import com.pc.ai.AiDetectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * Step 4 — AI authorship analysis.
 *
 * <p>Runs {@link AiDetectorService} on the raw text and stores the
 * {@code aiProbability} in the execution context. The Score step
 * will use this value when persisting {@code similarity_results}.
 *
 * <p>Note: Do not call AI APIs directly inside this step —
 * delegate to the @Service bean (AiDetectorService) so unit tests
 * can mock the service without making real API calls.
 */
@Component
public class AiAnalysisStep implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisStep.class);

    private final AiDetectorService aiDetector;

    public AiAnalysisStep(AiDetectorService aiDetector) {
        this.aiDetector = aiDetector;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution,
                                ChunkContext chunkContext) throws Exception {
        var ctx = chunkContext.getStepContext().getStepExecution()
                              .getJobExecution().getExecutionContext();

        String rawText      = (String) ctx.get("rawText");
        String submissionId = (String) ctx.get("submissionId");

        if (rawText == null || rawText.isBlank()) {
            log.warn("AiAnalysisStep: no rawText for {}", submissionId);
            ctx.putDouble("aiProbability", 0.0);
            return RepeatStatus.FINISHED;
        }

        log.info("AiAnalysisStep: running AI detection for submission {}", submissionId);

        try {
            AiDetectorService.DetectionResult result = aiDetector.detect(rawText);
            ctx.putDouble("aiProbability", result.aiProbability());
            log.info("AiAnalysisStep: submission {} → AI probability {:.2f} ({})",
                    submissionId, result.aiProbability(), result.riskLabel());
        } catch (Exception e) {
            // Non-fatal: store 0.0 and continue
            log.warn("AiAnalysisStep: detection failed for {}: {}", submissionId, e.getMessage());
            ctx.putDouble("aiProbability", 0.0);
        }

        contribution.incrementWriteCount(1);
        return RepeatStatus.FINISHED;
    }
}
