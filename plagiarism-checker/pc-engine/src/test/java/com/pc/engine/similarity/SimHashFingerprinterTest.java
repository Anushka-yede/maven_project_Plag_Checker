package com.pc.engine.similarity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class SimHashFingerprinterTest {

    private final SimHashFingerprinter fp = new SimHashFingerprinter();

    @Test
    void identicalTexts_haveSimilarity1() {
        String text = "the quick brown fox jumps over the lazy dog";
        long a = fp.fingerprint(text);
        long b = fp.fingerprint(text);
        assertEquals(1.0, fp.similarity(a, b), 0.0001);
    }

    @Test
    void completelyDifferentTexts_haveLowSimilarity() {
        long a = fp.fingerprint("apple orange banana grape");
        long b = fp.fingerprint("zymurgy xylophone violin trumpet");
        // Can't guarantee 0 but should be significantly below 1
        assertTrue(fp.similarity(a, b) < 0.9);
    }

    @Test
    void nearDuplicates_haveHighSimilarity() {
        String text1 = "the quick brown fox jumps over the lazy dog";
        String text2 = "the quick brown fox leaps over the lazy dog";
        double sim = fp.similarity(text1, text2);
        assertTrue(sim > 0.6, "Near-duplicate similarity should be > 0.6, got: " + sim);
    }

    @Test
    void hammingDistance_zeroForIdentical() {
        long a = fp.fingerprint("same text here");
        assertEquals(0, fp.hammingDistance(a, a));
    }
}
