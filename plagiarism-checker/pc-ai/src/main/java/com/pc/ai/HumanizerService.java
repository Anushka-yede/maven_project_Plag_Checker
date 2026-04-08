package com.pc.ai;

import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Feature 01 — AI Humanizer.
 *
 * <p>Takes a block of text (likely AI-generated) and rewrites it to sound
 * naturally human-written using Claude API. Preserves all factual content.
 *
 * <p>REST endpoint: {@code POST /api/humanize}
 */
@Service
public class HumanizerService {

    private final GeminiClient claude;

    public HumanizerService(GeminiClient claude) {
        this.claude = claude;
    }

    /**
     * Rewrites AI-generated text to sound naturally human.
     *
     * @param inputText the AI-generated text to humanize
     * @return rewritten, human-sounding version
     * @throws IOException if the Gemini API call fails
     */
    public String humanize(String inputText) throws IOException {
        if (inputText == null || inputText.isBlank()) {
            throw new IllegalArgumentException("Input text must not be blank");
        }
        if (inputText.length() > 10_000) {
            throw new IllegalArgumentException("Input text exceeds 10,000 character limit");
        }

        String prompt = """
                You are an expert editor. The following text is suspected to be AI-generated.
                Rewrite it so it sounds naturally human-written:
                - Vary sentence length significantly (mix short punchy sentences with longer ones)
                - Add natural hedging ('it seems', 'arguably', 'in most cases')
                - Remove robotic transition words ('Furthermore', 'In conclusion', 'It is important to note')
                - Introduce minor imperfections a human might write
                - Preserve all factual content and meaning exactly
                - Do NOT add any commentary. Return only the rewritten text.

                TEXT TO HUMANISE:
                %s
                """.formatted(inputText);

        return claude.complete(prompt);
    }

    /**
     * Returns a diff-friendly pair: original and humanized text.
     */
    public HumanizeResult humanizeWithDiff(String inputText) throws IOException {
        String humanized = humanize(inputText);
        return new HumanizeResult(inputText, humanized);
    }

    /** DTO returned to the REST layer. */
    public record HumanizeResult(String original, String humanized) {}
}
