package com.jwtdemo.controller;

import com.jwtdemo.model.*;
import com.jwtdemo.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Auth endpoints — all are public (no JWT needed to access these)
 *
 * POST /api/auth/login   → authenticate and get tokens
 * POST /api/auth/refresh → exchange refresh token for new access token
 * POST /api/auth/logout  → client-side logout (clear token)
 * POST /api/auth/inspect → decode and explain a token (for learning)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // ── POST /api/auth/login ──────────────────────────────────────
    /**
     * Step 1: Client sends username + password
     * Step 2: Spring Security authenticates (checks UserDetailsService)
     * Step 3: If valid, generate access + refresh tokens
     * Step 4: Return tokens with full explanation of what each contains
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            log.info("Login attempt for user: {}", request.getUsername());

            // Authenticate — throws BadCredentialsException if wrong
            Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );

            UserDetails userDetails = (UserDetails) auth.getPrincipal();

            // Generate both tokens
            String accessToken  = jwtService.generateAccessToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            // Decode for display (educational — shows what's inside the token)
            Map<String, Object> tokenInfo = jwtService.decodeTokenForDisplay(accessToken);

            // Split token into its 3 parts for the UI to display
            String[] parts = accessToken.split("\\.");
            String decodedHeader  = new String(Base64.getUrlDecoder().decode(padBase64(parts[0])));
            String decodedPayload = new String(Base64.getUrlDecoder().decode(padBase64(parts[1])));

            AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900)         // 15 minutes in seconds
                .username(userDetails.getUsername())
                .roles(userDetails.getAuthorities().stream()
                    .map(a -> a.getAuthority()).toList())
                // Educational: show the decoded parts
                .jwtHeader(decodedHeader)
                .jwtPayload(decodedPayload)
                .jwtSignature(parts[2].substring(0, 20) + "...")  // truncated for display
                .tokenInfo(tokenInfo)
                .message("Login successful! Token generated and ready to use.")
                .build();

            log.info("Login successful for user: {}", request.getUsername());
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "error", "Invalid username or password",
                    "hint", "Try: admin/admin123 or suresh/password"
                ));
        }
    }

    // ── POST /api/auth/refresh ────────────────────────────────────
    /**
     * Exchange a refresh token for a new access token.
     * The refresh token is long-lived (24h), access token is short-lived (15min).
     * This lets users stay logged in without re-entering credentials.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "refreshToken is required"));
        }

        try {
            // Verify token type
            String tokenType = jwtService.extractTokenType(refreshToken);
            if (!"REFRESH".equals(tokenType)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not a refresh token"));
            }

            // Extract username and load user
            String username = jwtService.extractUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Validate refresh token
            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh token is expired or invalid"));
            }

            // Issue new access token
            String newAccessToken = jwtService.generateAccessToken(userDetails);
            Map<String, Object> tokenInfo = jwtService.decodeTokenForDisplay(newAccessToken);

            return ResponseEntity.ok(Map.of(
                "accessToken",  newAccessToken,
                "tokenType",    "Bearer",
                "expiresIn",    900,
                "tokenInfo",    tokenInfo,
                "message",      "New access token issued from refresh token"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid refresh token: " + e.getMessage()));
        }
    }

    // ── POST /api/auth/inspect ─────────────────────────────────────
    /**
     * Educational endpoint: decode and explain any JWT token.
     * Shows all three parts and what each claim means.
     */
    @PostMapping("/inspect")
    public ResponseEntity<?> inspectToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "token is required"));
        }

        try {
            String[] parts = token.trim().split("\\.");
            if (parts.length != 3) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Not a valid JWT — must have 3 parts separated by dots"));
            }

            String decodedHeader  = new String(Base64.getUrlDecoder().decode(padBase64(parts[0])));
            String decodedPayload = new String(Base64.getUrlDecoder().decode(padBase64(parts[1])));
            Map<String, Object> claims = jwtService.decodeTokenForDisplay(token);

            return ResponseEntity.ok(Map.of(
                "rawToken",       token,
                "parts",          Map.of(
                    "header",     parts[0],
                    "payload",    parts[1],
                    "signature",  parts[2]
                ),
                "decoded",        Map.of(
                    "header",     decodedHeader,
                    "payload",    decodedPayload,
                    "signature",  "HMAC-SHA256 signature (cannot be decoded without the secret key)"
                ),
                "claims",         claims,
                "explanation",    Map.of(
                    "sub",  "Subject — who this token represents (the username)",
                    "iat",  "Issued At — Unix timestamp when token was created",
                    "exp",  "Expires At — Unix timestamp when token becomes invalid",
                    "roles","Roles/authorities this user has",
                    "type", "Token type: ACCESS (short-lived) or REFRESH (long-lived)",
                    "jti",  "JWT ID — unique identifier for this specific token"
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Failed to inspect token: " + e.getMessage()));
        }
    }

    // ── POST /api/auth/logout ──────────────────────────────────────
    /**
     * JWT logout is client-side only — just delete the token from localStorage.
     * In production, you'd also add the token to a denylist (Redis).
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Server-side: nothing to do for basic JWT
        // Production: add token JTI to Redis denylist with TTL = token expiry
        return ResponseEntity.ok(Map.of(
            "message", "Logged out. Delete the JWT from localStorage on the client.",
            "note", "In production, the token JTI would be added to a Redis denylist."
        ));
    }

    private String padBase64(String base64) {
        int padding = 4 - base64.length() % 4;
        if (padding != 4) {
            base64 += "=".repeat(padding);
        }
        return base64;
    }
}
