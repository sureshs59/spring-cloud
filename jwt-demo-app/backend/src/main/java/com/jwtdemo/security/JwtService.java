package com.jwtdemo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════════
 * JWT SERVICE — The Heart of JWT Authentication
 * ═══════════════════════════════════════════════════════════════════
 *
 * A JWT (JSON Web Token) has 3 parts separated by dots:
 *
 *   HEADER.PAYLOAD.SIGNATURE
 *
 *   eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9          ← HEADER (Base64)
 *   .eyJzdWIiOiJzdXJlc2giLCJyb2xlcyI6WyJVU0VSIl19  ← PAYLOAD (Base64)
 *   .SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c   ← SIGNATURE (HMAC-SHA256)
 *
 * HEADER:    { "alg": "HS256", "typ": "JWT" }
 * PAYLOAD:   { "sub": "suresh", "roles": ["USER"], "iat": 1234, "exp": 5678 }
 * SIGNATURE: HMAC-SHA256(base64(header) + "." + base64(payload), secretKey)
 *
 * The signature proves the token was issued by us and not tampered with.
 * ═══════════════════════════════════════════════════════════════════
 */
@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @Value("${jwt.refresh-exp-ms}")
    private long refreshExpirationMs;

    // ── Step 1: Generate the signing key from our secret ──────────
    // The secret string is Base64-encoded and turned into an HMAC-SHA256 key
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
            Base64.getEncoder().encodeToString(secretKey.getBytes())
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ── Step 2: Generate ACCESS TOKEN ─────────────────────────────
    /**
     * Creates a short-lived access token (15 minutes).
     *
     * Claims embedded in the token:
     *   sub   → username (the "subject" — who this token represents)
     *   roles → user's authorities (e.g. ["ROLE_USER", "ROLE_ADMIN"])
     *   iat   → issued at (Unix timestamp — when token was created)
     *   exp   → expires at (Unix timestamp — when token becomes invalid)
     *   jti   → JWT ID — unique ID for this specific token (prevent replay)
     *   type  → "ACCESS" — distinguishes from refresh token
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();

        // Add roles to the token payload
        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        extraClaims.put("roles", roles);
        extraClaims.put("type", "ACCESS");
        extraClaims.put("jti", UUID.randomUUID().toString()); // unique token ID

        return buildToken(extraClaims, userDetails, expirationMs);
    }

    // ── Step 3: Generate REFRESH TOKEN ────────────────────────────
    /**
     * Creates a long-lived refresh token (24 hours).
     * Used to get a new access token without logging in again.
     * Contains minimal claims — just enough to identify the user.
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("type", "REFRESH");
        extraClaims.put("jti", UUID.randomUUID().toString());
        return buildToken(extraClaims, userDetails, refreshExpirationMs);
    }

    // ── Step 4: Build the actual JWT ──────────────────────────────
    /**
     * This is where the JWT is constructed:
     *
     *   1. Set claims (payload data)
     *   2. Set subject (username)
     *   3. Set issued-at timestamp
     *   4. Set expiry timestamp
     *   5. Sign with HMAC-SHA256 using our secret key
     *   6. Compact to a string (header.payload.signature)
     */
    private String buildToken(Map<String, Object> extraClaims,
                               UserDetails userDetails,
                               long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        String token = Jwts.builder()
                .claims(extraClaims)            // PAYLOAD: custom claims (roles, type, jti)
                .subject(userDetails.getUsername()) // PAYLOAD: "sub" claim
                .issuedAt(now)                  // PAYLOAD: "iat" claim
                .expiration(expiryDate)         // PAYLOAD: "exp" claim
                .signWith(getSigningKey())       // SIGNATURE: HMAC-SHA256
                .compact();                     // build: header.payload.signature

        log.debug("Generated JWT for user '{}', expires at: {}", userDetails.getUsername(), expiryDate);
        return token;
    }

    // ── Step 5: Extract username from token ───────────────────────
    /**
     * The "sub" (subject) claim holds the username.
     * This lets us identify WHO is making the request.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // ── Step 6: Extract expiry from token ─────────────────────────
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // ── Step 7: Extract roles from token ──────────────────────────
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object roles = claims.get("roles");
        if (roles instanceof List) {
            return (List<String>) roles;
        }
        return List.of();
    }

    // ── Step 8: Extract token type ────────────────────────────────
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    // ── Step 9: Validate the token ────────────────────────────────
    /**
     * Validates by checking:
     *   1. Username matches the expected user
     *   2. Token is not expired
     *   3. Signature is valid (implicitly checked by extractAllClaims)
     *   4. Token has not been tampered with
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            boolean usernameMatch = username.equals(userDetails.getUsername());
            boolean notExpired    = !isTokenExpired(token);

            log.debug("Token validation — username match: {}, not expired: {}",
                    usernameMatch, notExpired);

            return usernameMatch && notExpired;
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ── Step 10: Extract all claims (parse the token) ─────────────
    /**
     * This is the core parsing step:
     *   1. Split token into header.payload.signature
     *   2. Base64-decode the header → verify algorithm
     *   3. Base64-decode the payload → extract claims
     *   4. Verify HMAC-SHA256 signature using our secret key
     *   5. If signature is invalid → JwtException thrown
     *   6. If token is expired → ExpiredJwtException thrown
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())     // verify HMAC-SHA256 signature
                .build()
                .parseSignedClaims(token)        // parse and validate
                .getPayload();                   // return the payload (claims)
    }

    // ── Generic claim extractor ────────────────────────────────────
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ── Debug helper: decode token without verification ────────────
    /**
     * Returns the decoded header and payload (without verifying signature).
     * Used by the frontend Token Inspector to show what's inside the token.
     * NEVER use this for authentication — it doesn't verify the signature!
     */
    public Map<String, Object> decodeTokenForDisplay(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Map<String, Object> result = new LinkedHashMap<>();

            // Header info
            result.put("algorithm", "HS256");
            result.put("type", "JWT");

            // Payload claims
            result.put("subject", claims.getSubject());
            result.put("roles", claims.get("roles"));
            result.put("tokenType", claims.get("type"));
            result.put("issuedAt", claims.getIssuedAt().toString());
            result.put("expiresAt", claims.getExpiration().toString());
            result.put("jwtId", claims.get("jti"));

            long remainingSeconds = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
            result.put("remainingSeconds", Math.max(0, remainingSeconds));
            result.put("isExpired", isTokenExpired(token));

            return result;
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}
