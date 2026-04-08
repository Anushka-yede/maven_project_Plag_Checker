package com.pc.api.controller;

import com.pc.ai.AiDetectorService;
import com.pc.ai.CitationFixerService;
import com.pc.ai.HumanizerService;
import com.pc.ai.RewriteSuggesterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI features controller.
 *
 * POST /api/humanize        — Feature 01: rewrite AI text to sound human
 * POST /api/detect-ai       — Feature 02: AI authorship probability
 * POST /api/suggestions     — Feature 17: rewrite suggestions for matched spans
 * POST /api/citations       — Feature 04: citation suggestions
 * POST /api/check-preview   — quick pre-submission check (Chrome extension, Feature 20)
 */
@RestController
@RequestMapping("/api")
public class HumanizerController {

    private final HumanizerService        humanizer;
    private final AiDetectorService       detector;
    private final RewriteSuggesterService suggester;
    private final CitationFixerService    citationFixer;

    public HumanizerController(HumanizerService humanizer,
                                AiDetectorService detector,
                                RewriteSuggesterService suggester,
                                CitationFixerService citationFixer) {
        this.humanizer     = humanizer;
        this.detector      = detector;
        this.suggester     = suggester;
        this.citationFixer = citationFixer;
    }

    // ── POST /api/humanize ────────────────────────────────────────────────
    @PostMapping("/humanize")
    public ResponseEntity<?> humanize(@Valid @RequestBody TextRequest req) {
        try {
            HumanizerService.HumanizeResult result = humanizer.humanizeWithDiff(req.text());
            return ResponseEntity.ok(Map.of(
                    "original",  result.original(),
                    "humanized", result.humanized()
            ));
        } catch (IllegalArgumentException e) {
            return problem(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            return problem(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        } catch (Exception e) {
            return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Humanizer error: " + e.getMessage());
        }
    }

    // ── POST /api/detect-ai ───────────────────────────────────────────────
    @PostMapping("/detect-ai")
    public ResponseEntity<?> detectAi(@Valid @RequestBody TextRequest req) {
        try {
            AiDetectorService.DetectionResult result = detector.detect(req.text());
            return ResponseEntity.ok(Map.of(
                    "aiProbability",    result.aiProbability(),
                    "probabilityPct",   result.probabilityPercent(),
                    "riskLabel",        result.riskLabel(),
                    "reasoning",        result.reasoning(),
                    "indicators",       result.indicators()
            ));
        } catch (IllegalStateException e) {
            return problem(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        } catch (Exception e) {
            return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Detector error: " + e.getMessage());
        }
    }

    // ── POST /api/suggestions ─────────────────────────────────────────────
    @PostMapping("/suggestions")
    public ResponseEntity<?> suggest(@Valid @RequestBody SuggestionRequest req) {
        try {
            List<String> rewrites = suggester.suggest(req.matchedSpan(), req.context());
            return ResponseEntity.ok(Map.of(
                    "originalSpan", req.matchedSpan(),
                    "rewrites",     rewrites
            ));
        } catch (IllegalStateException e) {
            return problem(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        } catch (Exception e) {
            return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Suggester error: " + e.getMessage());
        }
    }

    // ── POST /api/citations ───────────────────────────────────────────────
    @PostMapping("/citations")
    public ResponseEntity<?> citations(@Valid @RequestBody TextRequest req) {
        try {
            List<CitationFixerService.CitationSuggestion> suggestions =
                    citationFixer.suggest(req.text());
            return ResponseEntity.ok(Map.of(
                    "uncitedSentences", suggestions.size(),
                    "suggestions",      suggestions
            ));
        } catch (Exception e) {
            return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Citation error: " + e.getMessage());
        }
    }

    // ── POST /api/check-preview ───────────────────────────────────────────
    // Feature 20 — Chrome extension pre-submission check
    @PostMapping("/check-preview")
    public ResponseEntity<?> checkPreview(@Valid @RequestBody TextRequest req) {
        try {
            // Run statistical AI detection (no Claude call — fast response for extension)
            double statScore = detector.statisticalScore(req.text());
            String risk = statScore >= 0.75 ? "HIGH" : statScore >= 0.45 ? "MEDIUM" : "LOW";
            return ResponseEntity.ok(Map.of(
                    "text",          req.text().substring(0, Math.min(req.text().length(), 100)) + "...",
                    "aiRisk",        risk,
                    "aiProbability", statScore,
                    "message",       "Pre-submission check complete. Risk: " + risk
            ));
        } catch (Exception e) {
            return problem(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        return ResponseEntity.status(status).body(pd);
    }

    // ── Request records ────────────────────────────────────────────────────

    public record TextRequest(
            @NotBlank @Size(min = 10, max = 10000) String text
    ) {}

    public record SuggestionRequest(
            @NotBlank String matchedSpan,
            String context
    ) {}
}
