package com.pc.api.controller;

import com.pc.api.entity.UserEntity;
import com.pc.api.repository.UserRepository;
import com.pc.api.security.JwtProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Authentication controller.
 * POST /api/auth/register — create account, return JWT
 * POST /api/auth/login    — authenticate, return JWT
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository         userRepo;
    private final PasswordEncoder        encoder;
    private final AuthenticationManager  authManager;
    private final JwtProvider            jwtProvider;

    public AuthController(UserRepository userRepo,
                          PasswordEncoder encoder,
                          AuthenticationManager authManager,
                          JwtProvider jwtProvider) {
        this.userRepo    = userRepo;
        this.encoder     = encoder;
        this.authManager = authManager;
        this.jwtProvider = jwtProvider;
    }

    // ── POST /api/auth/register ──────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepo.existsByEmail(req.email())) {
            return problem(HttpStatus.CONFLICT, "Email already registered: " + req.email());
        }
        UserEntity user = new UserEntity(
                req.email(),
                encoder.encode(req.password()),
                req.role() != null ? req.role() : "STUDENT",
                req.fullName()
        );
        userRepo.save(user);

        String token = jwtProvider.generateToken(req.email());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, user.getEmail(), user.getRole()));
    }

    // ── POST /api/auth/login ─────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email(), req.password()));
            String token = jwtProvider.generateToken(auth);
            UserEntity user = userRepo.findByEmail(req.email()).orElseThrow();
            return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getRole()));
        } catch (BadCredentialsException e) {
            return problem(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status).body(pd);
    }

    // ── Request / Response records ────────────────────────────────────────

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8) String password,
            String fullName,
            String role   // STUDENT | INSTRUCTOR | ADMIN (admin creates these)
    ) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record AuthResponse(String token, String email, String role) {}
}
