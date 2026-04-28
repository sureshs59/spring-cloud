package com.example.oauth2.service;

import com.example.oauth2.dto.AuthResponse;
import com.example.oauth2.dto.LoginRequest;
import com.example.oauth2.dto.RegisterRequest;
import com.example.oauth2.model.AuthProvider;
import com.example.oauth2.model.Role;
import com.example.oauth2.model.User;
import com.example.oauth2.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles local authentication (register + login with email/password).
 * OAuth2 authentication is handled by CustomOAuth2UserService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository       userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider     jwtTokenProvider;
    private final PasswordEncoder      passwordEncoder;

    /**
     * Register a new local user with email + password.
     * Throws if email already exists.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email address already in use: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))  // BCrypt hash
                .provider(AuthProvider.local)
                .emailVerified(false)
                .role(Role.ROLE_USER)
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        // Auto-login after registration
        return loginInternal(request.getEmail(), request.getPassword());
    }

    /**
     * Authenticate a local user with email + password.
     * Returns a JWT token on success.
     */
    public AuthResponse login(LoginRequest request) {
        return loginInternal(request.getEmail(), request.getPassword());
    }

    private AuthResponse loginInternal(String email, String password) {
        // Spring Security validates email + password via DaoAuthenticationProvider
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT for this authentication
        String jwt = jwtTokenProvider.generateToken(authentication);
        log.info("User logged in: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return AuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }
}
