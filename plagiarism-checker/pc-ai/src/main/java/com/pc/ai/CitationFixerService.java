package com.pc.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Feature 04 — Citation Fixer.
 *
 * <p>Detects sentences that contain lifted facts or quotes without citations.
 * Suggests APA/MLA/Chicago citation strings by querying CrossRef API.
 *
 * <p>REST endpoint: {@code POST /api/citations} (body: {text})
 */
@Service
public class CitationFixerService {

    private static final String CROSSREF_URL = "https://api.crossref.org/works?query=";
    private static final Pattern CITATION_PATTERN =
            Pattern.compile("\\(.*?\\d{4}.*?\\)|\\[\\d+\\]");

    private final GeminiClient claude;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;

    public CitationFixerService(GeminiClient claude) {
        this.claude = claude;
        this.httpClient = new OkHttpClient();
        this.mapper = new ObjectMapper();
    }

    /**
     * Analyses text, detects uncited sentences, and returns citation suggestions.
     *
     * @param documentText full document text
     * @return list of flagged sentences with citation suggestions
     */
    public List<CitationSuggestion> suggest(String documentText) throws IOException {
        List<CitationSuggestion> suggestions = new ArrayList<>();
        String[] sentences = documentText.split("(?<=[.!?])\\s+");

        for (String sentence : sentences) {
            if (sentence.trim().length() < 40) continue;
            if (likelyCited(sentence)) continue;
            if (likelyNeedsCitation(sentence)) {
                List<String> refs = queryCrossRef(extractKeyTerms(sentence));
                if (!refs.isEmpty()) {
                    suggestions.add(new CitationSuggestion(sentence.trim(), refs));
                }
            }
        }
        return suggestions;
    }

    /**
     * Uses Claude to identify which sentences need citations in a block of text.
     */
    public List<String> detectUncitedSentences(String text) throws IOException {
        String prompt = """
                Analyse the following academic text. Identify sentences that contain
                factual claims, statistics, or quoted material that are NOT already cited.
                
                Return ONLY a numbered list of the uncited sentences (verbatim), one per line.
                If all sentences are properly cited, return "ALL_CITED".
                
                TEXT:
                %s
                """.formatted(text.length() > 3000 ? text.substring(0, 3000) : text);

        String response = claude.complete(prompt);
        if (response.contains("ALL_CITED")) return List.of();

        return List.of(response.split("\\n"))
                .stream()
                .map(l -> l.replaceAll("^\\d+\\.\\s*", "").trim())
                .filter(l -> !l.isBlank())
                .toList();
    }

    /**
     * Queries CrossRef API for papers matching the given keywords.
     * Returns formatted APA-style citations.
     */
    private List<String> queryCrossRef(String query) throws IOException {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        Request request = new Request.Builder()
                .url(CROSSREF_URL + encoded + "&rows=3")
                .header("User-Agent", "PlagiarismCheckerPro/1.0 (mailto:support@pc.com)")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return List.of();
            JsonNode root = mapper.readTree(response.body().string());
            JsonNode items = root.path("message").path("items");
            List<String> citations = new ArrayList<>();
            for (JsonNode item : items) {
                String citation = formatApa(item);
                if (!citation.isBlank()) citations.add(citation);
            }
            return citations;
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> queryCrossRef(List<String> keyTerms) throws IOException {
        return queryCrossRef(String.join(" ", keyTerms));
    }

    /**
     * Formats a CrossRef item as an APA citation string.
     */
    private String formatApa(JsonNode item) {
        try {
            String title = item.path("title").get(0).asText("");
            String year  = item.path("published").path("date-parts").get(0).get(0).asText("n.d.");
            String doi   = item.path("DOI").asText("");
            JsonNode authorsNode = item.path("author");
            StringBuilder authors = new StringBuilder();
            int count = 0;
            for (JsonNode a : authorsNode) {
                if (count++ > 0) authors.append(", ");
                authors.append(a.path("family").asText("")).append(", ")
                       .append(a.path("given").asText("").substring(0, 1)).append(".");
                if (count >= 3) { authors.append(" et al."); break; }
            }
            return String.format("%s (%s). %s. https://doi.org/%s", authors, year, title, doi);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Extracts key noun phrases from a sentence for CrossRef search.
     */
    private List<String> extractKeyTerms(String sentence) {
        // Simple extraction: words > 4 chars, exclude common stop words
        String[] stopWords = {"that","this","with","from","they","have","were","been","will","would"};
        java.util.Set<String> stops = java.util.Set.of(stopWords);
        return java.util.Arrays.stream(sentence.split("\\s+"))
                .map(w -> w.replaceAll("[^a-zA-Z]", ""))
                .filter(w -> w.length() > 4 && !stops.contains(w.toLowerCase()))
                .limit(5)
                .toList();
    }

    private boolean likelyCited(String sentence) {
        return CITATION_PATTERN.matcher(sentence).find();
    }

    private boolean likelyNeedsCitation(String sentence) {
        String lower = sentence.toLowerCase();
        return lower.contains("research show") || lower.contains("studies show") ||
               lower.contains("according to") || lower.contains("percent") ||
               lower.contains("found that") || lower.contains("reported") ||
               lower.matches(".*\\d+(\\.\\d+)?%.*") || sentence.contains("\"");
    }

    public record CitationSuggestion(String sentence, List<String> suggestedCitations) {}
}
