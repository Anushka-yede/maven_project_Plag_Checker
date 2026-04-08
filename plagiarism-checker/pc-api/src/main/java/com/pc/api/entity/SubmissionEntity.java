package com.pc.api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "submissions")
public class SubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String filename;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "sha256_hash", nullable = false)
    private String sha256Hash;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(nullable = false)
    private int version = 1;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "assignment_id")
    private String assignmentId;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public SubmissionEntity() {}

    // ── Getters & Setters ──
    public UUID    getId()                    { return id; }
    public String  getFilename()              { return filename; }
    public void    setFilename(String f)      { this.filename = f; }
    public String  getStorageKey()            { return storageKey; }
    public void    setStorageKey(String k)    { this.storageKey = k; }
    public String  getSha256Hash()            { return sha256Hash; }
    public void    setSha256Hash(String h)    { this.sha256Hash = h; }
    public UUID    getUserId()                { return userId; }
    public void    setUserId(UUID u)          { this.userId = u; }
    public UUID    getJobId()                 { return jobId; }
    public void    setJobId(UUID j)           { this.jobId = j; }
    public int     getVersion()               { return version; }
    public void    setVersion(int v)          { this.version = v; }
    public String  getStatus()               { return status; }
    public void    setStatus(String s)        { this.status = s; }
    public String  getAssignmentId()          { return assignmentId; }
    public void    setAssignmentId(String a)  { this.assignmentId = a; }
    public Instant getCreatedAt()             { return createdAt; }
}
