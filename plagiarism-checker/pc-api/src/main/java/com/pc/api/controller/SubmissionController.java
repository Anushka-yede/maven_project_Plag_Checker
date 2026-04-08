package com.pc.api.controller;

import com.pc.api.entity.SubmissionEntity;
import com.pc.api.service.SubmissionService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * Submission controller.
 *
 * POST   /api/submissions          — upload file, trigger batch job
 * GET    /api/submissions          — list current user's submissions (STUDENT)
 * GET    /api/submissions/{id}     — get submission metadata
 * GET    /api/submissions/{id}/integrity — verify tamper-proof hash (Feature 16)
 */
@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    // ── POST /api/submissions ─────────────────────────────────────────────
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "assignmentId", required = false) String assignmentId,
            Authentication auth) {
        try {
            SubmissionEntity entity = submissionService.ingest(file, auth.getName(), assignmentId);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(toDto(entity));
        } catch (IllegalArgumentException e) {
            return problem(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Upload failed: " + e.getMessage());
        }
    }

    // ── GET /api/submissions ──────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<SubmissionDto>> list(Authentication auth) {
        List<SubmissionDto> dtos = submissionService.findByUser(auth.getName())
                .stream().map(this::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    // ── GET /api/submissions/{id} ─────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id, Authentication auth) {
        return submissionService.findById(id)
                .map(s -> ResponseEntity.ok(toDto(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── GET /api/submissions/{id}/integrity ───────────────────────────────
    @GetMapping("/{id}/integrity")
    public ResponseEntity<?> checkIntegrity(@PathVariable UUID id) {
        try {
            boolean valid = submissionService.verifyIntegrity(id);
            return ResponseEntity.ok(Map.of(
                    "submissionId", id.toString(),
                    "integityValid", valid,
                    "message", valid ? "SHA-256 hash matches — file is unmodified"
                                     : "INTEGRITY VIOLATION — file hash mismatch!"
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return problem(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private SubmissionDto toDto(SubmissionEntity e) {
        return new SubmissionDto(e.getId(), e.getFilename(), e.getStatus(),
                e.getSha256Hash(), e.getVersion(), e.getAssignmentId(), e.getCreatedAt().toString());
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        return ResponseEntity.status(status).body(pd);
    }

    public record SubmissionDto(
            UUID id, String filename, String status,
            String sha256Hash, int version, String assignmentId, String createdAt
    ) {}
}
