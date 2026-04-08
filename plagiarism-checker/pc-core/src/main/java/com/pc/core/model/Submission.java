package com.pc.core.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable domain record representing a submitted document.
 * Stored in the {@code submissions} table.
 */
public record Submission(
        UUID id,
        String filename,
        String storageKey,       // MinIO object key
        String sha256Hash,       // tamper-proof receipt
        UUID userId,
        UUID jobId,
        int version,             // re-submission version counter
        String status,           // PENDING | PROCESSING | DONE | FAILED
        Instant createdAt
) {
    /** Factory for creating a new pending submission before DB insert. */
    public static Submission create(String filename, String storageKey,
                                    String sha256Hash, UUID userId) {
        return new Submission(
                UUID.randomUUID(),
                filename,
                storageKey,
                sha256Hash,
                userId,
                null,
                1,
                "PENDING",
                Instant.now()
        );
    }

    public Submission withJobId(UUID jobId) {
        return new Submission(id, filename, storageKey, sha256Hash,
                userId, jobId, version, status, createdAt);
    }

    public Submission withStatus(String status) {
        return new Submission(id, filename, storageKey, sha256Hash,
                userId, jobId, version, status, createdAt);
    }
}
