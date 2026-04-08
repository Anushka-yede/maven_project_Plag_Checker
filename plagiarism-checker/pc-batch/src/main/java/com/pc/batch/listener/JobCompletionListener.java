package com.pc.batch.listener;

import com.pc.batch.listener.WebhookNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Spring Batch job listener that fires a webhook on job completion.
 * Delegates to {@link WebhookNotifier} so the HTTP call can be mocked in tests.
 */
@Component
public class JobCompletionListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobCompletionListener.class);

    private final WebhookNotifier webhookNotifier;

    public JobCompletionListener(WebhookNotifier webhookNotifier) {
        this.webhookNotifier = webhookNotifier;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Job '{}' starting (id={})",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getJobId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String submissionId = jobExecution.getExecutionContext().getString("submissionId", "unknown");
        BatchStatus status  = jobExecution.getStatus();

        log.info("Job '{}' finished with status {} for submission {}",
                jobExecution.getJobInstance().getJobName(), status, submissionId);

        if (status == BatchStatus.COMPLETED || status == BatchStatus.FAILED) {
            try {
                webhookNotifier.notify(submissionId, status.name());
            } catch (Exception e) {
                log.error("Webhook notification failed for submission {}: {}", submissionId, e.getMessage());
            }
        }
    }
}
