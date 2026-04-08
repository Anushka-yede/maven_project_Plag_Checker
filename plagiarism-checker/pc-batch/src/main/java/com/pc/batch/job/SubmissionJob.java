package com.pc.batch.job;

import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.pc.batch.listener.JobCompletionListener;
import com.pc.batch.step.*;

/**
 * Spring Batch job definition for processing a submitted document.
 *
 * <p>Pipeline: ParseStep → IndexStep → ScoreStep → AiAnalysisStep
 *
 * <p>Each step runs in isolation so a parse failure does not abort scoring.
 * The job uses chunk-oriented processing with skip policy for resilience.
 */
@Configuration
public class SubmissionJob {

    @Autowired private ParseStep parseStep;
    @Autowired private IndexStep indexStep;
    @Autowired private ScoreStep scoreStep;
    @Autowired private AiAnalysisStep aiAnalysisStep;
    @Autowired private JobCompletionListener completionListener;

    @Bean
    public Job submissionProcessingJob(JobRepository jobRepository,
                                        PlatformTransactionManager txManager) {
        Step parse = new StepBuilder("parseStep", jobRepository)
                .tasklet(parseStep, txManager)
                .build();

        Step index = new StepBuilder("indexStep", jobRepository)
                .tasklet(indexStep, txManager)
                .build();

        Step score = new StepBuilder("scoreStep", jobRepository)
                .tasklet(scoreStep, txManager)
                .build();

        Step aiAnalysis = new StepBuilder("aiAnalysisStep", jobRepository)
                .tasklet(aiAnalysisStep, txManager)
                .build();

        return new JobBuilder("submissionProcessingJob", jobRepository)
                .listener(completionListener)
                .start(parse)
                .next(index)
                .next(score)
                .next(aiAnalysis)
                .build();
    }
}
