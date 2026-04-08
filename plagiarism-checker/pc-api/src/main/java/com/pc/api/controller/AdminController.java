package com.pc.api.controller;

import com.pc.api.entity.SubmissionEntity;
import com.pc.api.repository.SubmissionRepository;
import com.pc.api.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Admin controller — ADMIN role only.
 *
 * GET  /api/admin/users              — list all users
 * PUT  /api/admin/users/{id}/role    — change user role
 * GET  /api/admin/submissions        — paginated submissions view
 * GET  /api/admin/stats              — system statistics
 * PUT  /api/admin/webhooks/{id}      — configure webhook per assignment (Feature 07/12)
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository       userRepo;
    private final SubmissionRepository submissionRepo;

    // In-memory webhook store. Replace with DB-backed WebhookRepository for production.
    private final Map<String, String> webhookStore = new java.util.concurrent.ConcurrentHashMap<>();

    public AdminController(UserRepository userRepo,
                           SubmissionRepository submissionRepo) {
        this.userRepo       = userRepo;
        this.submissionRepo = submissionRepo;
    }

    // ── GET /api/admin/users ──────────────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<List<UserView>> listUsers() {
        List<UserView> users = userRepo.findAll().stream()
                .map(u -> new UserView(u.getId(), u.getEmail(), u.getRole(),
                        u.getFullName(), u.getCreatedAt().toString()))
                .toList();
        return ResponseEntity.ok(users);
    }

    // ── PUT /api/admin/users/{id}/role ────────────────────────────────────
    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable UUID id,
                                        @RequestBody RoleRequest req) {
        return userRepo.findById(id).map(user -> {
            user.setRole(req.role());
            userRepo.save(user);
            return ResponseEntity.ok(Map.of("userId", id, "newRole", req.role()));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── GET /api/admin/submissions ────────────────────────────────────────
    @GetMapping("/submissions")
    public ResponseEntity<List<SubmissionEntity>> allSubmissions(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(
            submissionRepo.findAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending())
            ).getContent()
        );
    }

    // ── GET /api/admin/stats ──────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of(
                "totalUsers",       userRepo.count(),
                "totalSubmissions", submissionRepo.count(),
                "serverTime",       java.time.Instant.now().toString()
        ));
    }

    // ── PUT /api/admin/webhooks/{assignmentId} ────────────────────────────
    @PutMapping("/webhooks/{assignmentId}")
    public ResponseEntity<?> upsertWebhook(@PathVariable String assignmentId,
                                            @RequestBody WebhookRequest req) {
        if (req.webhookUrl() == null || req.webhookUrl().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "webhookUrl must not be blank"));
        }
        webhookStore.put(assignmentId, req.webhookUrl());
        return ResponseEntity.ok(Map.of(
                "assignmentId", assignmentId,
                "webhookUrl",   req.webhookUrl(),
                "status",       "saved"
        ));
    }

    // ── GET /api/admin/webhooks/{assignmentId} ────────────────────────────
    @GetMapping("/webhooks/{assignmentId}")
    public ResponseEntity<?> getWebhook(@PathVariable String assignmentId) {
        String url = webhookStore.get(assignmentId);
        if (url == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("assignmentId", assignmentId, "webhookUrl", url));
    }

    // ── Request / View records ────────────────────────────────────────────

    public record RoleRequest(@NotBlank String role) {}
    public record WebhookRequest(@NotBlank String webhookUrl) {}
    public record UserView(UUID id, String email, String role, String fullName, String createdAt) {}
}
