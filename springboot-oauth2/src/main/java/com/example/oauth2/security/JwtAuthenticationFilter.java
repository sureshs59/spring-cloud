package com.example.oauth2.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter — runs once per HTTP request.
 *
 * Flow:
 *  1. Extract Bearer token from Authorization header
 *  2. Validate the JWT signature and expiry
 *  3. Load UserDetails from database using email in the token
 *  4. Set the authentication in the SecurityContext
 *  5. Continue the filter chain
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider    jwtTokenProvider;
    private final UserDetailsService  userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {

        try {
            // Step 1: Extract JWT from "Authorization: Bearer <token>" header
            String jwt = extractJwtFromRequest(request);

            // Step 2: Validate and authenticate
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {

                // Step 3: Get user email from token
                String email = jwtTokenProvider.getEmailFromToken(jwt);

                // Step 4: Load user details
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Step 5: Create authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,                        // credentials not needed after validation
                                userDetails.getAuthorities()
                        );
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                // Step 6: Put auth in SecurityContext — this request is now authenticated
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user: {}", email);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        // Always continue the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extract the JWT from the Authorization header.
     * Expected format: "Authorization: Bearer eyJhb..."
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);   // strip "Bearer " prefix
        }
        return null;
    }
}
