package com.pc.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Feature 02 — AI Authorship Detector.
 *
 * <p>Returns a probability score 0.0 (definitely human) to 1.0 (definitely AI).
 *
 * <p>Blends two signals:
 * <ol>
 *   <li>Statistical: perplexity (unigram log-likelihood) + burstiness variance</li>
 *   <li>Claude classification: structured JSON response with ai_probability</li>
 * </ol>
 *
 * <p>REST endpoint: {@code POST /api/detect-ai}
 */
@Service
public class AiDetectorService {

    private final GeminiClient claude;
    private final ObjectMapper mapper;

    public AiDetectorService(GeminiClient claude) {
        this.claude = claude;
        this.mapper = new ObjectMapper();
    }

    /**
     * Analyses text and returns an AI-authorship detection result.
     *
     * @param text raw document text
     * @return {@link DetectionResult} with probability and indicators
     */
    public DetectionResult detect(String text) throws IOException {
        if (text == null || text.isBlank()) {
            return new DetectionResult(0.0, "Insufficient text", List.of());
        }

        // Signal 1: Statistical (perplexity + burstiness)
        double statScore = statisticalScore(text);

        // Signal 2: Claude classification
        ClaudeClassification claude_result = claudeClassify(text);

        // Blend: 40% statistical, 60% Gemini
        double blended = (statScore * 0.40) + (claude_result.aiProbability() * 0.60);
        blended = Math.max(0.0, Math.min(1.0, blended));

        List<String> indicators = new ArrayList<>(claude_result.indicators());
        if (statScore > 0.70) indicators.add("low sentence-length variance (statistical)");
        if (statScore > 0.85) indicators.add("low burstiness score (statistical)");

        return new DetectionResult(blended, claude_result.reasoning(), indicators);
    }

    /**
     * Statistical AI detection: perplexity proxy + burstiness.
     * Low perplexity + low burstiness → high AI probability.
     */
    public double statisticalScore(String text) {
        String[] sentences = text.split("(?<=[.!?])\\s+");
        if (sentences.length < 3) return 0.5; // insufficient data

        // Burstiness: coefficient of variation of sentence lengths
        double[] lengths = new double[sentences.length];
        double sum = 0;
        for (int i = 0; i < sentences.length; i++) {
            lengths[i] = sentences[i].split("\\s+").length;
            sum += lengths[i];
        }
        double mean = sum / lengths.length;

        double variance = 0;
        for (double l : lengths) variance += Math.pow(l - mean, 2);
        variance /= lengths.length;
        double stdDev = Math.sqrt(variance);

        // Coefficient of variation: high CV = human (bursty), low CV = AI
        double cv = mean > 0 ? stdDev / mean : 0;

        // Vocabulary richness: type-token ratio
        String[] words = text.toLowerCase().split("\\s+");
        long uniqueWords = java.util.Arrays.stream(words).distinct().count();
        double ttr = words.length > 0 ? (double) uniqueWords / words.length : 0;

        // Scoring: low CV and low TTR → high AI probability
        double burstScore = 1.0 - Math.min(cv, 1.0);         // 0=human bursty, 1=AI flat
        double vocabScore = 1.0 - Math.min(ttr * 2.0, 1.0);  // 0=human diverse, 1=AI repetitive

        return (burstScore * 0.6 + vocabScore * 0.4);
    }

    /**
     * Gemini-side classification — returns structured JSON.
     */
    private ClaudeClassification claudeClassify(String text) throws IOException {
        // Truncate to 3000 chars for API efficiency
        String truncated = text.length() > 3000 ? text.substring(0, 3000) + "..." : text;

        String prompt = """
                Analyse the following text and determine if it was written by a human or AI.
                Consider: sentence length variance, vocabulary diversity, naturalness of flow,
                presence of hedging language, and stylistic consistency.

                Respond ONLY with a JSON object, no other text:
                {
                  "ai_probability": 0.87,
                  "reasoning": "short explanation",
                  "indicators": ["uniform sentence length", "no hedging"]
                }

                TEXT:
                %s
                """.formatted(truncated);

        String response = claude.complete(prompt);

        // Parse JSON response
        try {
            // Strip markdown code blocks if present
            String jsonStr = response.trim();
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.replaceAll("```json\\n?|```\\n?", "").trim();
            }
            JsonNode node = mapper.readTree(jsonStr);
            double prob = node.path("ai_probability").asDouble(0.5);
            String reasoning = node.path("reasoning").asText("Unable to determine");
            List<String> indicators = new ArrayList<>();
            node.path("indicators").forEach(n -> indicators.add(n.asText()));
            return new ClaudeClassification(prob, reasoning, indicators);
        } catch (Exception e) {
            // Fallback if JSON parse fails
            return new ClaudeClassification(0.5, "Parse error: " + e.getMessage(), List.of());
        }
    }

    // ──────────────── Result Records ────────────────

    public record DetectionResult(
            double aiProbability,
            String reasoning,
            List<String> indicators
    ) {
        public String riskLabel() {
            if (aiProbability >= 0.80) return "HIGH — likely AI-generated";
            if (aiProbability >= 0.50) return "MEDIUM — possibly AI-assisted";
            return "LOW — likely human-written";
        }

        public int probabilityPercent() {
            return (int) Math.round(aiProbability * 100);
        }
    }

    private record ClaudeClassification(double aiProbability, String reasoning, List<String> indicators) {}
}
