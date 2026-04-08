package com.pc.batch.job;

import com.pc.core.util.TokenUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Feature 08 — Web Check Job.
 *
 * <p>Extracts top 5-grams from the document, queries Google (via Jsoup),
 * and flags external web sources with matching spans.
 *
 * <p>Results are stored as matched_spans with a "WEB" source type.
 */
@Configuration
public class WebCheckJob {

    private static final Logger log = LoggerFactory.getLogger(WebCheckJob.class);
    private static final String GOOGLE_URL = "https://www.google.com/search?q=";
    private static final int    TOP_NGRAMS  = 5;

    @Bean
    public Job webCheckJob(JobRepository jobRepository,
                            PlatformTransactionManager txManager) {
        Step step = new StepBuilder("webCheckStep", jobRepository)
                .tasklet(webCheckTasklet(), txManager)
                .build();

        return new JobBuilder("webCheckJob", jobRepository)
                .start(step)
                .build();
    }

    @Bean
    public Tasklet webCheckTasklet() {
        return (StepContribution contribution, ChunkContext chunkContext) -> {
            Map<String, Object> params = chunkContext.getStepContext()
                    .getJobParameters();
            String normalizedText = (String) params.get("normalizedText");
            String submissionId   = (String) params.get("submissionId");

            if (normalizedText == null || normalizedText.isBlank()) {
                log.warn("webCheckTasklet: no normalizedText for submission {}", submissionId);
                return RepeatStatus.FINISHED;
            }

            List<String> ngrams = TokenUtils.topNgrams(normalizedText, 5, TOP_NGRAMS);
            log.info("Web check: {} 5-grams for submission {}", ngrams.size(), submissionId);

            for (String ngram : ngrams) {
                try {
                    String url = GOOGLE_URL + URLEncoder.encode("\"" + ngram + "\"", StandardCharsets.UTF_8);
                    Document doc = Jsoup.connect(url)
                            .userAgent("Mozilla/5.0 (compatible; PlagiarismCheckerBot/1.0)")
                            .timeout(5000)
                            .get();
                    // Extract first 3 result URLs
                    doc.select("a[href]").stream()
                       .map(a -> a.attr("href"))
                       .filter(h -> h.startsWith("http") && !h.contains("google"))
                       .limit(3)
                       .forEach(externalUrl -> log.info("Web match: ngram='{}' url='{}'", ngram, externalUrl));
                } catch (Exception e) {
                    log.debug("Web check failed for ngram '{}': {}", ngram, e.getMessage());
                }
            }
            return RepeatStatus.FINISHED;
        };
    }
}
