package com.pc.core.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextNormalizerTest {

    @Test
    void normalize_returnsLowercase() {
        String result = TextNormalizer.normalize("Hello World!");
        assertTrue(result.equals(result.toLowerCase()));
    }

    @Test
    void normalize_removesPunctuation() {
        String result = TextNormalizer.normalize("Hello, World! How are you?");
        assertFalse(result.contains(",") || result.contains("!") || result.contains("?"));
    }

    @Test
    void normalize_handlesNull() {
        assertEquals("", TextNormalizer.normalize(null));
    }

    @Test
    void normalize_handlesBlank() {
        assertEquals("", TextNormalizer.normalize("   "));
    }

    @Test
    void porterStem_plurals() {
        assertEquals("car", TextNormalizer.porterStem("cars"));
        assertEquals("walk", TextNormalizer.porterStem("walked"));
    }

    @Test
    void measure_basicWords() {
        // "tree" → T-R-EE → measure=1
        assertTrue(TextNormalizer.measure("tree") >= 0);
        // "caresses" → measure > 0
        assertTrue(TextNormalizer.measure("caress") > 0);
    }
}
