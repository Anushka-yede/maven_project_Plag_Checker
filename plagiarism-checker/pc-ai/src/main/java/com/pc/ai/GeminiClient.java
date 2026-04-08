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
 * Low-level OkHttp wrapper for the Google Gemini API.
 *
 * <p>Security: API key read from environment variable {@code GOOGLE_API_KEY}
 * via Spring {@code @Value}. Never hardcoded.
 *
 * <p>Model: {@code gemini-2.0-flash}, maxOutputTokens: 2048.
 */
@Component
public class GeminiClient {

    private static final String BASE_URL   = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final int    MAX_TOKENS = 2048;
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;
    private final String apiKey;

    public GeminiClient(@Value("${gemini.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(120))
                .writeTimeout(Duration.ofSeconds(30))
                .build();
        this.mapper = new ObjectMapper();
    }

    /**
     * Sends a single user prompt to Gemini and returns the text response.
     *
     * @param userPrompt the complete prompt to send
     * @return Gemini's text response
     * @throws IOException           on network failure
     * @throws IllegalStateException if GOOGLE_API_KEY is not configured
     */
    public String complete(String userPrompt) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "GOOGLE_API_KEY is not set. " +
                "Export it as an environment variable before starting the application.");
        }

        // Build request body: { contents: [{ parts: [{ text: "..." }] }], generationConfig: { maxOutputTokens: 2048 } }
        ObjectNode body = mapper.createObjectNode();

        ArrayNode contents = body.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        ObjectNode part = parts.addObject();
        part.put("text", userPrompt);

        ObjectNode generationConfig = body.putObject("generationConfig");
        generationConfig.put("maxOutputTokens", MAX_TOKENS);

        String json = mapper.writeValueAsString(body);

        String url = BASE_URL + "?key=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .header("content-type", "application/json")
                .post(RequestBody.create(json, JSON_MEDIA))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "(no body)";
                throw new IOException("Gemini API error " + response.code() + ": " + errorBody);
            }
            String responseBody = response.body().string();
            JsonNode root = mapper.readTree(responseBody);
            return root.path("candidates").get(0)
                       .path("content").path("parts").get(0)
                       .path("text").asText();
        }
    }

    /**
     * Returns true if the API key is configured (for health checks).
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
