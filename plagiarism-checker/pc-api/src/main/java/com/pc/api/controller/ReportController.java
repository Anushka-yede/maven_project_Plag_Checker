package com.pc.api.controller;

import com.pc.api.entity.*;
import com.pc.api.repository.AnnotationRepository;
import com.pc.api.repository.UserRepository;
import com.pc.api.service.ReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Report controller.
 *
 * GET  /api/reports/{id}                 — full similarity report
 * GET  /api/reports/{id}/export          — PDF audit report (Feature 10)
 * GET  /api/reports/cohort/{assignmentId}— N×N heatmap data (Feature 11)
 * POST /api/annotations                  — save reviewer annotation (Feature 09)
 */
@RestController
@RequestMapping("/api")
public class ReportController {

    private final ReportService        reportService;
    private final AnnotationRepository annotationRepo;
    private final UserRepository       userRepo;

    public ReportController(ReportService reportService,
                            AnnotationRepository annotationRepo,
                            UserRepository userRepo) {
        this.reportService   = reportService;
        this.annotationRepo  = annotationRepo;
        this.userRepo        = userRepo;
    }

    // ── GET /api/reports/{id} ─────────────────────────────────────────────
    @GetMapping("/reports/{id}")
    public ResponseEntity<?> getReport(@PathVariable UUID id, Authentication auth) {
        try {
            ReportService.ReportDto report = reportService.getReport(id);
            // STUDENT role sees only own report, blurred source names (Feature 15)
            boolean isStudent = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

            List<ResultView> results = report.results().stream()
                    .map(r -> toResultView(r, isStudent))
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "submissionId", id,
                    "results",      results,
                    "spans",        report.spans().stream().map(this::toSpanView).toList(),
                    "annotations",  report.annotations().stream().map(this::toAnnotationView).toList()
            ));
        } catch (Exception e) {
            return problem(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // ── GET /api/reports/{id}/export ──────────────────────────────────────
    @GetMapping("/reports/{id}/export")
    public ResponseEntity<byte[]> exportPdf(@PathVariable UUID id) {
        try {
            byte[] pdf = reportService.exportPdf(id);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"report-" + id + ".pdf\"")
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── GET /api/reports/cohort/{assignmentId} ────────────────────────────
    @GetMapping("/reports/cohort/{assignmentId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public ResponseEntity<List<ReportService.HeatmapCell>> getCohortHeatmap(
            @PathVariable String assignmentId) {
        return ResponseEntity.ok(reportService.getCohortHeatmap(assignmentId));
    }

    // ── POST /api/annotations ─────────────────────────────────────────────
    @PostMapping("/annotations")
    public ResponseEntity<?> addAnnotation(@Valid @RequestBody AnnotationRequest req,
                                           Authentication auth) {
        String email = auth.getName();
        UUID reviewerId = userRepo.findByEmail(email)
                .map(UserEntity::getId)
                .orElse(null);

        AnnotationEntity ann = new AnnotationEntity();
        ann.setResultId(req.resultId());
        ann.setReviewerId(reviewerId);
        ann.setSpanStart(req.spanStart());
        ann.setVerdict(req.verdict());
        ann.setNote(req.note());

        AnnotationEntity saved = annotationRepo.save(ann);
        return ResponseEntity.status(HttpStatus.CREATED).body(toAnnotationView(saved));
    }

    // ── View projections (Role-based, Feature 15) ─────────────────────────

    private ResultView toResultView(SimilarityResultEntity r, boolean blurSourceNames) {
        String docBLabel = blurSourceNames
                ? "Document-" + r.getDocBId().toString().substring(0, 4)
                : r.getDocBId().toString();
        return new ResultView(
                r.getId(), r.getDocAId(), docBLabel,
                r.getTfidfScore(), r.getSimhashScore(),
                r.getSemanticScore(), r.getAiProbability(),
                r.getFinalScore(), r.getAlgorithm(), r.getMatchedAt().toString()
        );
    }

    private Map<String, Object> toSpanView(MatchedSpanEntity s) {
        return Map.of(
                "id", s.getId(), "startChar", s.getStartChar(),
                "endChar", s.getEndChar(),
                "matchedText", s.getMatchedText() != null ? s.getMatchedText() : "",
                "sourceType", s.getSourceType()
        );
    }

    private Map<String, Object> toAnnotationView(AnnotationEntity a) {
        return Map.of(
                "id", a.getId(), "verdict", a.getVerdict() != null ? a.getVerdict() : "",
                "note", a.getNote() != null ? a.getNote() : "",
                "createdAt", a.getCreatedAt().toString()
        );
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        return ResponseEntity.status(status).body(pd);
    }

    // ── Request / View records ────────────────────────────────────────────

    public record AnnotationRequest(
            UUID resultId,
            int spanStart,
            @NotBlank @Pattern(regexp = "confirmed|dismissed|grey_area") String verdict,
            String note
    ) {}

    public record ResultView(
            UUID id, UUID docAId, String docBLabel,
            Object tfidfScore, Object simhashScore, Object semanticScore,
            Object aiProbability, Object finalScore, String algorithm, String matchedAt
    ) {}
}
