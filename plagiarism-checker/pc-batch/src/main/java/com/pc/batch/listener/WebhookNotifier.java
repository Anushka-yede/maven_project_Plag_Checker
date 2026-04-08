package com.pc.batch.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Feature 13 — Webhook Notifier with exponential backoff retry.
 *
 * <p>On batch job completion, fires a configurable {@code POST} to the
 * webhook URL stored per-assignment. Retries up to 5 times with
 * exponential backoff starting at 1 second.
 *
 * <p>Payload: {@code {submissionId, finalScore, status, timestamp}}
 */
@Component
public class WebhookNotifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotifier.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int MAX_RETRIES = 5;

    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;

    @Value("${webhook.default-url:}")
    private String defaultWebhookUrl;

    public WebhookNotifier() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    /**
     * Fires the webhook notification with retry.
     *
     * @param submissionId the submission UUID
     * @param status       job status string (COMPLETED / FAILED)
     */
    public void notify(String submissionId, String status) {
        notify(submissionId, status, 0.0, defaultWebhookUrl);
    }

    /**
     * Fires the webhook to a specific URL.
     *
     * @param submissionId the submission UUID
     * @param status       job status string
     * @param finalScore   computed similarity final score
     * @param webhookUrl   target URL (if null/blank, notification is skipped)
     */
    public void notify(String submissionId, String status, double finalScore, String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("No webhook URL configured for submission {}", submissionId);
            return;
        }

        ObjectNode payload = mapper.createObjectNode();
        payload.put("submissionId", submissionId);
        payload.put("finalScore",   finalScore);
        payload.put("status",       status);
        payload.put("timestamp",    Instant.now().toString());

        String json;
        try {
            json = mapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to serialize webhook payload: {}", e.getMessage());
            return;
        }

        fireWithRetry(webhookUrl, json, 0);
    }

    private void fireWithRetry(String url, String json, int attempt) {
        if (attempt >= MAX_RETRIES) {
            log.error("Webhook to {} failed after {} attempts", url, MAX_RETRIES);
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, JSON))
                .header("Content-Type", "application/json")
                .header("X-PC-Source", "plagiarism-checker")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                log.info("Webhook delivered to {} (attempt {})", url, attempt + 1);
            } else {
                log.warn("Webhook to {} returned {}, retrying...", url, response.code());
                sleepExponential(attempt);
                fireWithRetry(url, json, attempt + 1);
            }
        } catch (IOException e) {
            log.warn("Webhook to {} failed: {}, retrying...", url, e.getMessage());
            sleepExponential(attempt);
            fireWithRetry(url, json, attempt + 1);
        }
    }

    private void sleepExponential(int attempt) {
        long delayMs = (long) (1000 * Math.pow(2, attempt));   // 1s, 2s, 4s, 8s, 16s
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
