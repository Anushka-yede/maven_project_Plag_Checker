package com.pc.parser;

import java.util.Map;

/**
 * Factory that selects the appropriate {@link DocumentParser} based on
 * file extension or MIME type.
 *
 * <p>Supports: pdf, docx, doc, txt, md, java, py, js, ts, cpp, c, cs
 */
public final class ParserFactory {

    private static final Map<String, DocumentParser> PARSERS = Map.of(
            "pdf",  new PdfParser(),
            "docx", new DocxParser(),
            "doc",  new DocxParser(),   // .doc treated as OOXML (works for modern .doc)
            "txt",  new TxtParser(),
            "md",   new TxtParser(),
            "java", new TxtParser(),
            "py",   new TxtParser(),
            "js",   new TxtParser(),
            "ts",   new TxtParser(),
            "cpp",  new TxtParser()
    );

    private ParserFactory() {}

    /**
     * Returns the parser for the given filename, selecting by extension.
     *
     * @param filename original filename with extension
     * @return appropriate parser
     * @throws IllegalArgumentException if file type is not supported
     */
    public static DocumentParser forFilename(String filename) {
        String ext = extension(filename);
        DocumentParser parser = PARSERS.get(ext);
        if (parser == null) {
            throw new IllegalArgumentException(
                "Unsupported file type: ." + ext + 
                ". Supported types: " + String.join(", ", PARSERS.keySet()));
        }
        return parser;
    }

    /**
     * Returns a parser for the given MIME type, falling back to extension lookup.
     */
    public static DocumentParser forMimeType(String mimeType, String filename) {
        return switch (mimeType) {
            case "application/pdf"                                              -> new PdfParser();
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                 "application/msword"                                           -> new DocxParser();
            case "text/plain", "text/x-java-source", "text/x-python",
                 "application/javascript", "text/markdown"                      -> new TxtParser();
            default -> forFilename(filename);
        };
    }

    private static String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }
}
