package com.example.oauth2.controller;

import com.example.oauth2.dto.AuthResponse;
import com.example.oauth2.dto.LoginRequest;
import com.example.oauth2.dto.RegisterRequest;
import com.example.oauth2.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for local authentication.
 *
 * POST /api/auth/register  — create account + get JWT
 * POST /api/auth/login     — login + get JWT
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user with name, email, password.
     * Returns a JWT token immediately — no separate login step needed.
     *
     * Request body:
     * {
     *   "name":     "Suresh",
     *   "email":    "suresh@example.com",
     *   "password": "password123"
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * Login with email + password. Returns JWT.
     *
     * Request body:
     * {
     *   "email":    "suresh@example.com",
     *   "password": "password123"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
