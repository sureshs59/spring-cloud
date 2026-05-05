package com.jwtdemo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════════
 * JWT AUTHENTICATION FILTER
 * ═══════════════════════════════════════════════════════════════════
 *
 * Runs on EVERY HTTP request BEFORE Spring Security's regular filters.
 * Its job: check if there's a valid JWT in the Authorization header,
 * and if so, tell Spring Security "this user is authenticated."
 *
 * Flow:
 *   1. Request arrives: GET /api/dashboard
 *   2. Filter extracts "Bearer <token>" from Authorization header
 *   3. JwtService validates the token (signature + expiry)
 *   4. Username extracted from token → load user from DB (or memory)
 *   5. Set authentication in SecurityContext → Spring knows who this is
 *   6. Request continues to the controller
 *
 * If no token or invalid token:
 *   → SecurityContext stays empty
 *   → Spring Security returns 401 Unauthorized (for protected routes)
 * ═══════════════════════════════════════════════════════════════════
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // ── Step 1: Check for Authorization header ────────────────
        final String authHeader = request.getHeader("Authorization");

        // If no Authorization header or doesn't start with "Bearer ", skip
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No JWT token found for request: {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 2: Extract the JWT token ─────────────────────────
        // Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
        //                        ^--- this part is the JWT
        final String jwt = authHeader.substring(7); // skip "Bearer "

        try {
            // ── Step 3: Extract username from token ───────────────
            final String username = jwtService.extractUsername(jwt);
            log.debug("JWT found for user: {}", username);

            // ── Step 4: Only authenticate if not already done ─────
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Load the user from our store (in-memory for this demo)
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // ── Step 5: Validate the token ─────────────────────
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    // Extract roles from token and convert to authorities
                    List<SimpleGrantedAuthority> authorities =
                        jwtService.extractRoles(jwt).stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    // ── Step 6: Create Authentication object ────────
                    // This tells Spring Security "this request is authenticated as {username}"
                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            userDetails,    // principal (the user)
                            null,           // credentials (null — we used JWT, not password)
                            authorities     // granted authorities (roles)
                        );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // ── Step 7: Set in SecurityContext ───────────────
                    // From this point, Spring Security knows who is making the request
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Successfully authenticated user: {} with roles: {}", username, authorities);

                } else {
                    log.warn("Invalid JWT token for user: {}", username);
                }
            }

        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
            // Don't throw — let the request continue (Spring Security will return 401)
        }

        // Continue to the next filter in the chain
        filterChain.doFilter(request, response);
    }
}
