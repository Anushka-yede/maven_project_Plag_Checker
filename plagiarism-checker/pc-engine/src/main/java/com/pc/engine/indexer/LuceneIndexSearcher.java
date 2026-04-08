package com.pc.engine.indexer;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Searches the Lucene FSDirectory index for documents similar to a query.
 * Returns submission IDs of the top-K most similar documents.
 */
public class LuceneIndexSearcher implements Closeable {

    private final DirectoryReader reader;
    private final IndexSearcher searcher;
    private final StandardAnalyzer analyzer;

    public LuceneIndexSearcher(Path indexDir) throws IOException {
        FSDirectory dir = FSDirectory.open(indexDir);
        this.reader   = DirectoryReader.open(dir);
        this.searcher = new IndexSearcher(reader);
        this.analyzer = new StandardAnalyzer();
    }

    /**
     * Searches the index for documents similar to the provided normalized text.
     *
     * @param normalizedText stemmed, lowercased query text
     * @param topK           maximum number of results to return
     * @return ordered list of (submissionId, score) pairs
     */
    public List<SearchHit> search(String normalizedText, int topK) throws IOException {
        // Truncate to first 1024 tokens to avoid Lucene query length limit
        String queryText = truncate(normalizedText, 1024);
        Query query;
        try {
            query = new QueryParser("content", analyzer).parse(
                    QueryParser.escape(queryText));
        } catch (ParseException e) {
            return Collections.emptyList();
        }

        TopDocs topDocs = searcher.search(query, topK);
        List<SearchHit> hits = new ArrayList<>();
        for (ScoreDoc sd : topDocs.scoreDocs) {
            Document doc = searcher.doc(sd.doc);
            hits.add(new SearchHit(doc.get("id"), sd.score));
        }
        return hits;
    }

    /**
     * Returns the raw term vector for the given submission ID (for cosine scoring).
     */
    public Terms getTermVector(String submissionId) throws IOException {
        Query query = new TermQuery(new Term("id", submissionId));
        TopDocs docs = searcher.search(query, 1);
        if (docs.scoreDocs.length == 0) return null;
        return reader.getTermVector(docs.scoreDocs[0].doc, "content");
    }

    /** Returns total number of documents in the index. */
    public int docCount() {
        return reader.numDocs();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    private String truncate(String text, int maxTokens) {
        String[] parts = text.split("\\s+");
        if (parts.length <= maxTokens) return text;
        return String.join(" ", Arrays.copyOf(parts, maxTokens));
    }

    /** Result value from a Lucene search. */
    public record SearchHit(String submissionId, float score) {}
}
