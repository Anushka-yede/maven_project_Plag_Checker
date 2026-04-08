package com.pc.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "similarity_results")
public class SimilarityResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "doc_a_id")
    private UUID docAId;

    @Column(name = "doc_b_id")
    private UUID docBId;

    @Column(name = "tfidf_score",    precision = 5, scale = 4)
    private BigDecimal tfidfScore = BigDecimal.ZERO;

    @Column(name = "simhash_score",  precision = 5, scale = 4)
    private BigDecimal simhashScore = BigDecimal.ZERO;

    @Column(name = "semantic_score", precision = 5, scale = 4)
    private BigDecimal semanticScore = BigDecimal.ZERO;

    @Column(name = "ai_probability", precision = 5, scale = 4)
    private BigDecimal aiProbability = BigDecimal.ZERO;

    @Column(name = "final_score",    precision = 5, scale = 4)
    private BigDecimal finalScore = BigDecimal.ZERO;

    @Column
    private String algorithm = "TFIDF+SIMHASH+SEMANTIC";

    @Column(name = "matched_at")
    private Instant matchedAt = Instant.now();

    public SimilarityResultEntity() {}

    // ── Getters & Setters ──
    public UUID       getId()              { return id; }
    public UUID       getDocAId()          { return docAId; }
    public void       setDocAId(UUID id)   { this.docAId = id; }
    public UUID       getDocBId()          { return docBId; }
    public void       setDocBId(UUID id)   { this.docBId = id; }
    public BigDecimal getTfidfScore()      { return tfidfScore; }
    public void       setTfidfScore(BigDecimal v)    { this.tfidfScore = v; }
    public BigDecimal getSimhashScore()    { return simhashScore; }
    public void       setSimhashScore(BigDecimal v)  { this.simhashScore = v; }
    public BigDecimal getSemanticScore()   { return semanticScore; }
    public void       setSemanticScore(BigDecimal v) { this.semanticScore = v; }
    public BigDecimal getAiProbability()   { return aiProbability; }
    public void       setAiProbability(BigDecimal v) { this.aiProbability = v; }
    public BigDecimal getFinalScore()      { return finalScore; }
    public void       setFinalScore(BigDecimal v)    { this.finalScore = v; }
    public String     getAlgorithm()       { return algorithm; }
    public void       setAlgorithm(String a){ this.algorithm = a; }
    public Instant    getMatchedAt()       { return matchedAt; }
}
