package com.pc.api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role = "STUDENT";   // STUDENT | INSTRUCTOR | ADMIN

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    // ── Constructors ──
    public UserEntity() {}

    public UserEntity(String email, String passwordHash, String role, String fullName) {
        this.email        = email;
        this.passwordHash = passwordHash;
        this.role         = role;
        this.fullName     = fullName;
    }

    // ── Getters & Setters ──
    public UUID    getId()           { return id; }
    public String  getEmail()        { return email; }
    public void    setEmail(String e){ this.email = e; }
    public String  getPasswordHash() { return passwordHash; }
    public void    setPasswordHash(String h) { this.passwordHash = h; }
    public String  getRole()         { return role; }
    public void    setRole(String r) { this.role = r; }
    public String  getFullName()     { return fullName; }
    public void    setFullName(String n) { this.fullName = n; }
    public Instant getCreatedAt()    { return createdAt; }
}
