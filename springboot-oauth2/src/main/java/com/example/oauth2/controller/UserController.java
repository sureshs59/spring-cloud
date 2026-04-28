package com.example.oauth2.controller;

import com.example.oauth2.dto.UserProfileResponse;
import com.example.oauth2.model.User;
import com.example.oauth2.service.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Protected endpoints — all require a valid JWT in the Authorization header.
 *
 * GET  /api/user/me     — get the current authenticated user's profile
 * GET  /api/user/all    — get all users (ADMIN only)
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * Get the currently authenticated user's profile.
     *
     * Usage:
     *   GET http://localhost:8080/api/user/me
     *   Authorization: Bearer <your-jwt-token>
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(mapToProfile(user));
    }

    /**
     * Get all users — ADMIN role required.
     *
     * Usage:
     *   GET http://localhost:8080/api/user/all
     *   Authorization: Bearer <admin-jwt-token>
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        List<UserProfileResponse> users = userRepository.findAll()
                .stream()
                .map(this::mapToProfile)
                .toList();
        return ResponseEntity.ok(users);
    }

    private UserProfileResponse mapToProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .imageUrl(user.getImageUrl())
                .provider(user.getProvider().name())
                .role(user.getRole().name())
                .build();
    }
}
