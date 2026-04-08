package com.pc.core.model;

import java.util.UUID;

/**
 * Immutable domain record representing a matched text span between two documents.
 * Stored in {@code matched_spans}.
 */
public record Match(
        UUID id,
        UUID resultId,
        UUID docId,
        int startChar,
        int endChar,
        String matchedText
) {
    public static Match create(UUID resultId, UUID docId,
                               int startChar, int endChar, String matchedText) {
        return new Match(UUID.randomUUID(), resultId, docId,
                startChar, endChar, matchedText);
    }

    /** Returns the character length of this matched span. */
    public int length() {
        return endChar - startChar;
    }

    /** Returns a preview of the matched text (first 100 chars). */
    public String preview() {
        if (matchedText == null) return "";
        return matchedText.length() > 100
                ? matchedText.substring(0, 100) + "…"
                : matchedText;
    }
}
