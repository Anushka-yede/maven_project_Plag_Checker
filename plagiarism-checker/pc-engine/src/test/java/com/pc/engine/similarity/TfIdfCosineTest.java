package com.pc.engine.similarity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TfIdfCosineTest {

    private final TfIdfCosineScorer scorer = new TfIdfCosineScorer();

    @Test
    void identicalTexts_scoreIsOne() {
        String text = "the cat sat on the mat";
        double score = scorer.score(text, text);
        assertEquals(1.0, score, 0.0001);
    }

    @Test
    void completelyDifferentTexts_scoreIsZero() {
        double score = scorer.score("apple orange mango", "computer algorithm matrix");
        assertTrue(score < 0.2, "Unrelated texts should score < 0.2, got " + score);
    }

    @Test
    void partialOverlap_scoreBetweenZeroAndOne() {
        String a = "the quick brown fox";
        String b = "the quick red fox";
        double score = scorer.score(a, b);
        assertTrue(score > 0.0 && score < 1.0,
                "Partial overlap should be in (0,1), got " + score);
    }

    @Test
    void emptyTexts_returnZero() {
        assertEquals(0.0, scorer.score("", "hello world"), 0.0001);
        assertEquals(0.0, scorer.score("hello world", ""), 0.0001);
    }
}
