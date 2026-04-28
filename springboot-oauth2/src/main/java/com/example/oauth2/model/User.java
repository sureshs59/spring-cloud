package com.example.oauth2.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * User entity that supports TWO login strategies:
 *
 *   1. LOCAL   — registered with email + password (stored as BCrypt hash)
 *   2. GOOGLE  — authenticated via Google OAuth2 (no password stored)
 *   3. GITHUB  — authenticated via GitHub OAuth2 (no password stored)
 *
 * The provider field tells us which strategy was used.
 */
@Entity
@Table(name = "users",
       uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Password is only set for LOCAL users.
     * OAuth2 users have null here — never exposed in responses.
     */
    @Column
    private String password;

    /**
     * Which OAuth2 provider (or "local") was used to create this account.
     * Values: "local", "google", "github"
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    /**
     * The unique ID from the OAuth2 provider (sub from Google, id from GitHub).
     * Null for local users.
     */
    @Column
    private String providerId;

    /**
     * Profile picture URL from the OAuth2 provider.
     */
    @Column
    private String imageUrl;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.ROLE_USER;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
