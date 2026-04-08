package com.pc.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/**
 * Low-level OkHttp wrapper for the Anthropic Claude API.
 *
 * <p>Security: API key read from environment variable {@code ANTHROPIC_API_KEY}
 * via Spring {@code @Value}. Never hardcoded.
 *
 * <p>Model: {@code claude-sonnet-4-20250514}, max_tokens: 2048.
 */
@Component
public class ClaudeClient {

    private static final String API_URL   = "https://api.anthropic.com/v1/messages";
    private static final String MODEL     = "claude-sonnet-4-20250514";
    private static final int    MAX_TOKENS = 2048;
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;
    private final String apiKey;

    public ClaudeClient(@Value("${anthropic.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(120))
                .writeTimeout(Duration.ofSeconds(30))
                .build();
        this.mapper = new ObjectMapper();
    }

    /**
     * Sends a single user prompt to Claude and returns the text response.
     *
     * @param userPrompt the complete prompt to send
     * @return Claude's text response
     * @throws IOException          on network failure
     * @throws IllegalStateException if ANTHROPIC_API_KEY is not configured
     */
    public String complete(String userPrompt) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "ANTHROPIC_API_KEY is not set. " +
                "Export it as an environment variable before starting the application.");
        }

        ObjectNode body = mapper.createObjectNode();
        body.put("model", MODEL);
        body.put("max_tokens", MAX_TOKENS);

        ArrayNode messages = body.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        message.put("content", userPrompt);

        String json = mapper.writeValueAsString(body);

        Request request = new Request.Builder()
                .url(API_URL)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .post(RequestBody.create(json, JSON_MEDIA))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "(no body)";
                throw new IOException("Claude API error " + response.code() + ": " + errorBody);
            }
            String responseBody = response.body().string();
            JsonNode root = mapper.readTree(responseBody);
            return root.path("content").get(0).path("text").asText();
        }
    }

    /**
     * Returns true if the API key is configured (for health checks).
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
