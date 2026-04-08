package com.pc.engine.indexer;

import com.pc.core.util.TextNormalizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Writes normalized document text into a Lucene FSDirectory index.
 * Each document is stored with its submission UUID for later lookup.
 *
 * <p>Field layout:
 * <ul>
 *   <li>{@code id}      — stored StringField (submission UUID)</li>
 *   <li>{@code content} — TextField with term vectors (TF-IDF source)</li>
 * </ul>
 */
public class LuceneIndexWriter implements Closeable {

    private final IndexWriter writer;

    public LuceneIndexWriter(Path indexDir) throws IOException {
        FSDirectory dir = FSDirectory.open(indexDir);
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        this.writer = new IndexWriter(dir, config);
    }

    /**
     * Indexes a single submission. Skips if the submission ID already exists.
     *
     * @param submissionId UUID string of the submission
     * @param rawText      raw extracted text from the parser
     */
    public void index(String submissionId, String rawText) throws IOException {
        String normalizedText = TextNormalizer.normalize(rawText);

        Document doc = new Document();
        doc.add(new StringField("id", submissionId, Field.Store.YES));

        FieldType contentFieldType = new FieldType(TextField.TYPE_STORED);
        contentFieldType.setStoreTermVectors(true);
        contentFieldType.setStoreTermVectorPositions(true);
        contentFieldType.setStoreTermVectorOffsets(true);
        contentFieldType.freeze();

        doc.add(new Field("content", normalizedText, contentFieldType));

        // Delete old version if re-indexing
        writer.deleteDocuments(new Term("id", submissionId));
        writer.addDocument(doc);
        writer.commit();
    }

    /**
     * Removes a document from the index by submission ID.
     */
    public void delete(String submissionId) throws IOException {
        writer.deleteDocuments(new Term("id", submissionId));
        writer.commit();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
