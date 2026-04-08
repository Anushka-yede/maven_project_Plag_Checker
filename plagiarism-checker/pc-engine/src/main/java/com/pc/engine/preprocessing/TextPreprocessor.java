package com.pc.engine.preprocessing;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TextPreprocessor {
    
    // Basic stop words.
    private static final List<String> STOP_WORDS = Arrays.asList(
            "the", "is", "at", "which", "and", "on", "a", "an", "in", "to", "of", "for", "with", "by"
    );

    public String cleanText(String input) {
        if (input == null || input.isBlank()) return "";
        
        // 1. Lowercase
        String text = input.toLowerCase();
        
        // 2. Remove punctuation (keep only alphanumeric and spaces)
        text = text.replaceAll("[^a-z0-9\\s]", " ");
        
        // 3. Remove extra whitespaces
        text = text.replaceAll("\\s+", " ").trim();
        
        // 4. Remove Stop-words
        return Arrays.stream(text.split(" "))
                .filter(word -> !STOP_WORDS.contains(word))
                .collect(Collectors.joining(" "));
    }
}
