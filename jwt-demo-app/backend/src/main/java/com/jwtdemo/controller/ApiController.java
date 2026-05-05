package com.jwtdemo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Protected endpoints — require a valid JWT in Authorization header.
 *
 * These endpoints demonstrate:
 *   1. Simple authentication guard (any valid JWT)
 *   2. Role-based access control (@PreAuthorize)
 *   3. Accessing the authenticated user from SecurityContext
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    // ── GET /api/dashboard — requires any valid JWT ───────────────
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(Authentication auth) {
        return ResponseEntity.ok(Map.of(
            "message",     "Welcome to the protected dashboard, " + auth.getName() + "!",
            "user",        auth.getName(),
            "authorities", auth.getAuthorities().toString(),
            "timestamp",   LocalDateTime.now().toString(),
            "note",        "You can see this because your JWT was valid. The filter extracted your username from the token."
        ));
    }

    // ── GET /api/profile — shows what was decoded from JWT ────────
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication auth) {
        return ResponseEntity.ok(Map.of(
            "username",      auth.getName(),
            "roles",         auth.getAuthorities().stream()
                               .map(a -> a.getAuthority()).toList(),
            "authenticated", auth.isAuthenticated(),
            "explanation",   "This data was extracted from your JWT token — no database query was needed!"
        ));
    }

    // ── GET /api/admin — requires ROLE_ADMIN ─────────────────────
    @GetMapping("/admin/data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAdminData(Authentication auth) {
        return ResponseEntity.ok(Map.of(
            "secret",  "This is admin-only data!",
            "user",    auth.getName(),
            "message", "Your JWT contained ROLE_ADMIN, so you can access this endpoint."
        ));
    }

    // ── GET /api/public/info — no auth needed ─────────────────────
    @GetMapping("/public/info")
    public ResponseEntity<?> getPublicInfo() {
        return ResponseEntity.ok(Map.of(
            "message",  "This endpoint is public — no JWT needed.",
            "appName",  "JWT Demo App",
            "version",  "1.0.0",
            "jwtFlow",  new String[]{
                "1. POST /api/auth/login with username+password",
                "2. Receive JWT access token and refresh token",
                "3. Send 'Authorization: Bearer <token>' on protected requests",
                "4. Server validates signature and expiry",
                "5. Access granted if token is valid"
            }
        ));
    }
}
