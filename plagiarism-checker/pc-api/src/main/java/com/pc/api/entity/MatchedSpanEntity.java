package com.pc.api.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "matched_spans")
public class MatchedSpanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "result_id")
    private UUID resultId;

    @Column(name = "doc_id")
    private UUID docId;

    @Column(name = "start_char")
    private int startChar;

    @Column(name = "end_char")
    private int endChar;

    @Column(name = "matched_text", columnDefinition = "TEXT")
    private String matchedText;

    @Column(name = "source_type")
    private String sourceType = "CORPUS";  // CORPUS | WEB

    @Column(name = "source_url")
    private String sourceUrl;

    public MatchedSpanEntity() {}

    // ── Getters & Setters ──
    public UUID   getId()                  { return id; }
    public UUID   getResultId()            { return resultId; }
    public void   setResultId(UUID r)      { this.resultId = r; }
    public UUID   getDocId()               { return docId; }
    public void   setDocId(UUID d)         { this.docId = d; }
    public int    getStartChar()           { return startChar; }
    public void   setStartChar(int s)      { this.startChar = s; }
    public int    getEndChar()             { return endChar; }
    public void   setEndChar(int e)        { this.endChar = e; }
    public String getMatchedText()         { return matchedText; }
    public void   setMatchedText(String t) { this.matchedText = t; }
    public String getSourceType()          { return sourceType; }
    public void   setSourceType(String t)  { this.sourceType = t; }
    public String getSourceUrl()           { return sourceUrl; }
    public void   setSourceUrl(String u)   { this.sourceUrl = u; }
}
