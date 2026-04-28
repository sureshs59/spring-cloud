package com.example.oauth2.config;

import com.example.oauth2.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Main Spring Security configuration.
 *
 * Authentication modes:
 *  ┌──────────────────────────────────────────────────────────────┐
 *  │  1. LOCAL JWT     POST /api/auth/register                    │
 *  │                   POST /api/auth/login  → returns JWT        │
 *  │                   Use JWT in Authorization: Bearer header    │
 *  │                                                              │
 *  │  2. OAUTH2        GET /oauth2/authorize/google               │
 *  │                   GET /oauth2/authorize/github               │
 *  │                   → redirects to provider login              │
 *  │                   → on success → /oauth2/success?token=...   │
 *  └──────────────────────────────────────────────────────────────┘
 *
 * Public endpoints (no auth required):
 *   POST /api/auth/register
 *   POST /api/auth/login
 *   GET  /oauth2/**
 *   GET  /oauth2/callback/**
 *   GET  /oauth2/success
 *   GET  /h2-console/**
 *   GET  /actuator/health
 *
 * Protected endpoints (JWT required):
 *   GET  /api/user/me
 *   GET  /api/user/all      (ADMIN only)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService         userDetailsService;
    private final JwtAuthenticationFilter          jwtAuthFilter;
    private final CustomOAuth2UserService          customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;

    // ── Password Encoder (BCrypt) ─────────────────────────────────────────────
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ── Authentication Provider (for local login) ─────────────────────────────
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // ── Authentication Manager ────────────────────────────────────────────────
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ── CORS Configuration ────────────────────────────────────────────────────
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:4200",   // Angular dev server
                "http://localhost:3000",   // React dev server
                "http://localhost:8080"    // Same origin
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ── Main Security Filter Chain ────────────────────────────────────────────
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — we use JWT (stateless), not sessions
            .csrf(AbstractHttpConfigurer::disable)

            // Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Stateless session — no HttpSession created
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // URL authorisation rules
            .authorizeHttpRequests(auth -> auth
                    // Public endpoints — no token needed
                    .requestMatchers(
                            "/api/auth/**",          // login, register
                            "/oauth2/**",            // OAuth2 flow
                            "/login/oauth2/**",      // Spring Security OAuth2 callback
                            "/oauth2/success",       // success redirect page
                            "/h2-console/**",        // H2 browser console
                            "/actuator/health",      // health check
                            "/error"                 // error page
                    ).permitAll()

                    // Admin-only endpoints
                    .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")

                    // Everything else requires authentication
                    .anyRequest().authenticated()
            )

            // H2 console needs frames (same origin)
            .headers(headers ->
                    headers.frameOptions(frame -> frame.sameOrigin()))

            // OAuth2 login configuration
            .oauth2Login(oauth2 -> oauth2
                    .authorizationEndpoint(endpoint ->
                            endpoint.baseUri("/oauth2/authorize"))
                    .redirectionEndpoint(endpoint ->
                            endpoint.baseUri("/oauth2/callback/*"))
                    .userInfoEndpoint(userInfo ->
                            userInfo.userService(customOAuth2UserService::loadUser))
                    .successHandler(oauth2SuccessHandler)
            )

            // Register our custom authentication provider
            .authenticationProvider(authenticationProvider())

            // Add JWT filter before Spring's username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
