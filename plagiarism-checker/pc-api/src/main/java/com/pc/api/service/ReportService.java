package com.pc.api.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.pc.api.entity.*;
import com.pc.api.repository.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Report service — assembles similarity reports and exports them as PDF.
 * Feature 10 — PDF Audit Report export.
 */
@Service
public class ReportService {

    private final SimilarityResultRepository resultRepo;
    private final MatchedSpanRepository      spanRepo;
    private final AnnotationRepository       annotationRepo;
    private final SubmissionRepository       submissionRepo;

    public ReportService(SimilarityResultRepository resultRepo,
                         MatchedSpanRepository spanRepo,
                         AnnotationRepository annotationRepo,
                         SubmissionRepository submissionRepo) {
        this.resultRepo     = resultRepo;
        this.spanRepo       = spanRepo;
        this.annotationRepo = annotationRepo;
        this.submissionRepo = submissionRepo;
    }

    /**
     * Returns the full report for a submission: results + spans + annotations.
     */
    public ReportDto getReport(UUID submissionId) {
        List<SimilarityResultEntity> results =
                resultRepo.findByDocAIdOrDocBId(submissionId, submissionId);

        List<MatchedSpanEntity> spans = results.stream()
                .flatMap(r -> spanRepo.findByResultId(r.getId()).stream())
                .toList();

        List<AnnotationEntity> annotations = results.stream()
                .flatMap(r -> annotationRepo.findByResultId(r.getId()).stream())
                .toList();

        SubmissionEntity sub = submissionRepo.findById(submissionId).orElse(null);

        return new ReportDto(sub, results, spans, annotations);
    }

    /**
     * Returns N×N cohort heatmap data for all submissions in an assignment.
     * Feature 11 — Cohort Heatmap.
     */
    public List<HeatmapCell> getCohortHeatmap(String assignmentId) {
        List<SubmissionEntity> subs = submissionRepo.findByAssignmentIdOrderByCreatedAtDesc(assignmentId);
        List<UUID> ids = subs.stream().map(SubmissionEntity::getId).toList();
        List<SimilarityResultEntity> results = resultRepo.findByCohort(ids);

        return results.stream()
                .map(r -> new HeatmapCell(
                        r.getDocAId().toString(),
                        r.getDocBId().toString(),
                        r.getFinalScore().doubleValue()
                ))
                .toList();
    }

    /**
     * Exports a PDF audit report for a submission.
     * Feature 10 — PDF Audit Report (iText 8).
     */
    public byte[] exportPdf(UUID submissionId) throws Exception {
        ReportDto report = getReport(submissionId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer   = new PdfWriter(baos);
        PdfDocument pdf    = new PdfDocument(writer);
        Document document  = new Document(pdf);

        // Title
        Paragraph title = new Paragraph("Plagiarism Checker Pro — Audit Report")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        // Submission info
        if (report.submission() != null) {
            SubmissionEntity sub = report.submission();
            document.add(new Paragraph("Submission: " + sub.getFilename()).setFontSize(12));
            document.add(new Paragraph("Status: "   + sub.getStatus()).setFontSize(12));
            document.add(new Paragraph("SHA-256: "  + sub.getSha256Hash()).setFontSize(9)
                    .setFontColor(ColorConstants.GRAY));
            document.add(new Paragraph("Generated: " +
                    DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now())).setFontSize(10));
        }

        document.add(new Paragraph("\n"));

        // Results table
        document.add(new Paragraph("Similarity Results").setBold().setFontSize(14));
        Table table = new Table(new float[]{3, 2, 2, 2, 2, 2});
        table.addHeaderCell("Compared With").addHeaderCell("TF-IDF")
             .addHeaderCell("SimHash").addHeaderCell("Semantic")
             .addHeaderCell("AI Prob").addHeaderCell("Final Score");

        for (SimilarityResultEntity r : report.results()) {
            table.addCell(r.getDocBId().toString().substring(0, 8) + "...");
            table.addCell(r.getTfidfScore().toString());
            table.addCell(r.getSimhashScore().toString());
            table.addCell(r.getSemanticScore().toString());
            table.addCell(r.getAiProbability().toString());
            Cell scoreCell = new Cell().add(new Paragraph(r.getFinalScore().toString()));
            if (r.getFinalScore().doubleValue() >= 0.75)
                scoreCell.setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(255, 100, 100));
            else if (r.getFinalScore().doubleValue() >= 0.40)
                scoreCell.setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(255, 220, 100));
            table.addCell(scoreCell);
        }
        document.add(table);

        // Matched spans
        if (!report.spans().isEmpty()) {
            document.add(new Paragraph("\nMatched Spans").setBold().setFontSize(14));
            for (MatchedSpanEntity span : report.spans()) {
                String preview = span.getMatchedText() != null
                        ? (span.getMatchedText().length() > 120
                           ? span.getMatchedText().substring(0, 120) + "…"
                           : span.getMatchedText())
                        : "(empty)";
                document.add(new Paragraph("[ chars " + span.getStartChar()
                        + " – " + span.getEndChar() + " ] " + preview).setFontSize(9));
            }
        }

        // Annotations
        if (!report.annotations().isEmpty()) {
            document.add(new Paragraph("\nReviewer Annotations").setBold().setFontSize(14));
            for (AnnotationEntity ann : report.annotations()) {
                document.add(new Paragraph(
                        "[" + ann.getVerdict() + "] " + ann.getNote()).setFontSize(10));
            }
        }

        document.close();
        return baos.toByteArray();
    }

    // ──────────────── DTOs ────────────────

    public record ReportDto(
            SubmissionEntity submission,
            List<SimilarityResultEntity> results,
            List<MatchedSpanEntity> spans,
            List<AnnotationEntity> annotations
    ) {}

    public record HeatmapCell(String docAId, String docBId, double score) {}
}
