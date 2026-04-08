package com.pc.core.util;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TokenUtilsTest {

    @Test
    void tokenize_splitsOnWhitespace() {
        String[] tokens = TokenUtils.tokenize("hello world foo");
        assertArrayEquals(new String[]{"hello", "world", "foo"}, tokens);
    }

    @Test
    void wordNgrams_generatesTrigrams() {
        List<String> trigrams = TokenUtils.wordNgrams("the quick brown fox", 3);
        assertTrue(trigrams.contains("the quick brown"));
        assertTrue(trigrams.contains("quick brown fox"));
        assertEquals(2, trigrams.size());
    }

    @Test
    void wordNgrams_emptyInputReturnsEmpty() {
        List<String> result = TokenUtils.wordNgrams("", 3);
        assertTrue(result.isEmpty());
    }

    @Test
    void topNgrams_limitsResults() {
        String text = "one two three four five six seven eight nine ten";
        List<String> top = TokenUtils.topNgrams(text, 2, 3);
        assertEquals(3, top.size());
    }
}
