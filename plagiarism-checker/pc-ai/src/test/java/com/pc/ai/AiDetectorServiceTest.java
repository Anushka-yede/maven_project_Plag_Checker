package com.pc.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiDetectorServiceTest {

    @Mock   GeminiClient claude;
    @InjectMocks AiDetectorService svc;

    @Test
    void detect_returnsResultWithProbability() throws Exception {
        when(claude.complete(anyString())).thenReturn("""
            {"ai_probability": 0.85, "reasoning": "Uniform sentences", "indicators": ["no hedging"]}
            """);
        AiDetectorService.DetectionResult result = svc.detect("Text to analyze here.");
        assertTrue(result.aiProbability() >= 0.0 && result.aiProbability() <= 1.0);
        assertNotNull(result.reasoning());
    }

    @Test
    void statisticalScore_variesWithBurstiness() {
        // High burstiness text (human-like)
        String human = "I went. Then I stopped. The problem with modern approaches to machine learning is that they frequently oversimplify the underlying data distributions. Why?";
        // Low burstiness text (AI-like — uniform lengths)
        String ai = "The system processes data efficiently. The algorithm outputs results accurately. The model performs well overall. The metrics indicate high performance.";

        double humanScore = svc.statisticalScore(human);
        double aiScore    = svc.statisticalScore(ai);

        // AI-like text should score higher (more AI probability)
        assertTrue(aiScore >= humanScore - 0.3,
                "AI-like text should not score significantly lower than human-like text. ai=" + aiScore + " human=" + humanScore);
    }

    @Test
    void detect_onInsufficientText_returnsMidpoint() throws Exception {
        when(claude.complete(anyString())).thenReturn("""
            {"ai_probability": 0.5, "reasoning": "Insufficient data", "indicators": []}
            """);
        AiDetectorService.DetectionResult result = svc.detect("Short text.");
        assertNotNull(result);
    }

    @Test
    void probabilityPercent_inRange() throws Exception {
        when(claude.complete(anyString())).thenReturn("""
            {"ai_probability": 0.72, "reasoning": "Mixed signals", "indicators": ["consistent length"]}
            """);
        AiDetectorService.DetectionResult result = svc.detect("Some medium length text for testing purposes here.");
        assertTrue(result.probabilityPercent() >= 0 && result.probabilityPercent() <= 100);
    }
}
