package com.pc.parser;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Extracts plain text from DOCX documents using Apache POI.
 * Reads all paragraphs and table cells from the document body.
 */
public class DocxParser implements DocumentParser {

    @Override
    public String parse(InputStream input) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(input)) {
            StringBuilder sb = new StringBuilder();

            // Extract body paragraphs
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            for (XWPFParagraph para : paragraphs) {
                String text = para.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text).append('\n');
                }
            }

            // Extract table cell text
            doc.getTables().forEach(table ->
                table.getRows().forEach(row ->
                    row.getTableCells().forEach(cell -> {
                        String cellText = cell.getText();
                        if (cellText != null && !cellText.isBlank()) {
                            sb.append(cellText).append(' ');
                        }
                    })
                )
            );

            return sb.toString();
        }
    }

    @Override
    public String supportedExtension() {
        return "docx";
    }
}
