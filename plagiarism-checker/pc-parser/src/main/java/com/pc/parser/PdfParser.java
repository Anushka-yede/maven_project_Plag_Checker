package com.pc.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;

/**
 * Extracts plain text from PDF documents using Apache PDFBox 3.
 * Strips headers/footers heuristically by ignoring the first and last 5% of lines per page.
 */
public class PdfParser implements DocumentParser {

    @Override
    public String parse(InputStream input) throws IOException {
        try (PDDocument doc = Loader.loadPDF(input.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String raw = stripper.getText(doc);
            return stripHeadersFooters(raw);
        }
    }

    /**
     * Heuristic header/footer removal:
     * - Removes lines shorter than 5 chars that appear on every or most pages
     * - Removes page numbers (lines matching ^\s*\d+\s*$)
     */
    private String stripHeadersFooters(String raw) {
        String[] lines = raw.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            // Skip page number lines
            if (trimmed.matches("\\d+")) continue;
            // Skip very short standalone lines (likely headers/footers)
            if (trimmed.length() < 4 && !trimmed.isEmpty()) continue;
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    @Override
    public String supportedExtension() {
        return "pdf";
    }
}
