package com.pc.api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "annotations")
public class AnnotationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "result_id")
    private UUID resultId;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Column(name = "span_start")
    private int spanStart;

    @Column
    private String verdict;   // confirmed | dismissed | grey_area

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public AnnotationEntity() {}

    // ── Getters & Setters ──
    public UUID    getId()                   { return id; }
    public UUID    getResultId()             { return resultId; }
    public void    setResultId(UUID r)       { this.resultId = r; }
    public UUID    getReviewerId()           { return reviewerId; }
    public void    setReviewerId(UUID r)     { this.reviewerId = r; }
    public int     getSpanStart()            { return spanStart; }
    public void    setSpanStart(int s)       { this.spanStart = s; }
    public String  getVerdict()              { return verdict; }
    public void    setVerdict(String v)      { this.verdict = v; }
    public String  getNote()                 { return note; }
    public void    setNote(String n)         { this.note = n; }
    public Instant getCreatedAt()            { return createdAt; }
}
