package com.pc.ai;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Feature 17 — Smart Rewrite Suggester.
 *
 * <p>For each matched (plagiarised) span, generates 2 alternative rewrites
 * that preserve meaning but reduce similarity score. Called from the
 * DiffViewer UI on-demand.
 *
 * <p>REST endpoint: {@code POST /api/suggestions} (body: {span, context})
 */
@Service
public class RewriteSuggesterService {

    private final GeminiClient claude;

    public RewriteSuggesterService(GeminiClient claude) {
        this.claude = claude;
    }

    /**
     * Generates 2 alternative phrasings for a matched span.
     *
     * @param matchedSpan    the exact plagiarised text
     * @param surroundingCtx surrounding 200-char context for continuity
     * @return list of 2 alternative phrasings
     */
    public List<String> suggest(String matchedSpan, String surroundingCtx) throws IOException {
        if (matchedSpan == null || matchedSpan.isBlank()) {
            throw new IllegalArgumentException("Matched span must not be blank");
        }

        String context = surroundingCtx != null && !surroundingCtx.isBlank()
                ? surroundingCtx
                : "(no additional context)";

        String prompt = """
                You are helping a student rephrase a section of their paper to avoid plagiarism.
                Generate exactly 2 alternative phrasings of the MATCHED SPAN below.
                Each rewrite must:
                - Preserve the exact meaning and all factual claims
                - Use different vocabulary and sentence structure
                - Be natural academic writing
                - Be clearly labelled as "REWRITE 1:" and "REWRITE 2:"
                - Not include any other commentary

                SURROUNDING CONTEXT (for continuity):
                %s

                MATCHED SPAN TO REWRITE:
                %s
                """.formatted(context, matchedSpan);

        String response = claude.complete(prompt);
        return parseRewrites(response);
    }

    /**
     * Parses the "REWRITE 1: ... REWRITE 2: ..." format from Claude's response.
     */
    private List<String> parseRewrites(String response) {
        String[] parts = response.split("REWRITE [12]:");
        if (parts.length >= 3) {
            return List.of(parts[1].trim(), parts[2].trim());
        }
        // Fallback: split by double newline
        String[] lines = response.split("\\n\\n");
        if (lines.length >= 2) {
            return List.of(lines[0].trim(), lines[1].trim());
        }
        return List.of(response.trim(), response.trim());
    }

    /** DTO for the REST layer. */
    public record SuggestionResult(String originalSpan, List<String> rewrites) {}
}
