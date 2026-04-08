package com.pc.batch.step;

import com.pc.parser.DocumentParser;
import com.pc.parser.ParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.Map;

/**
 * Step 1 — Parse raw document bytes into plain text.
 *
 * <p>Reads {@code fileBytes} and {@code filename} from the job execution context,
 * invokes the correct parser via {@link ParserFactory}, and writes
 * {@code rawText} back into the execution context for downstream steps.
 */
@Component
public class ParseStep implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(ParseStep.class);

    @Override
    public RepeatStatus execute(StepContribution contribution,
                                ChunkContext chunkContext) throws Exception {
        var ctx = chunkContext.getStepContext().getStepExecution()
                              .getJobExecution().getExecutionContext();

        byte[] fileBytes = (byte[]) ctx.get("fileBytes");
        String filename  = (String) ctx.get("filename");

        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("No file bytes in execution context");
        }

        log.info("ParseStep: parsing file '{}'", filename);
        DocumentParser parser = ParserFactory.forFilename(filename);
        String rawText = parser.parse(new ByteArrayInputStream(fileBytes));

        if (rawText == null || rawText.isBlank()) {
            throw new IllegalStateException("Parser returned empty text for: " + filename);
        }

        ctx.putString("rawText", rawText);
        log.info("ParseStep: extracted {} chars from '{}'", rawText.length(), filename);
        contribution.incrementWriteCount(1);
        return RepeatStatus.FINISHED;
    }
}
