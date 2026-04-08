package com.pc.parser;

import java.io.IOException;
import java.io.InputStream;

/**
 * Common interface for all document parsers.
 */
public interface DocumentParser {

    /**
     * Extracts plain text from the given input stream.
     *
     * @param input raw bytes of the document
     * @return extracted plain text
     * @throws IOException on read failures
     */
    String parse(InputStream input) throws IOException;

    /**
     * Returns the file extension this parser handles (lowercase, no dot).
     * Examples: "pdf", "docx", "txt"
     */
    String supportedExtension();
}
