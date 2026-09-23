package com.speakup.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey signingKey;
    private final long expirationHours;

    public JwtTokenProvider(
            @Value("${jwt.secret:speakup-dev-jwt-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256}") String secret,
            @Value("${jwt.expiration-hours:168}") long expirationHours) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = expirationHours;
    }

    /**
     * Generate JWT token for an authenticated UserPrincipal.
     */
    public String generateToken(UserPrincipal principal) {
        String role = principal.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse("ROLE_USER");

        return generateToken(principal.getEmail(), principal.getId(), role);
    }

    /**
     * Generate JWT token from user details.
     */
    public String generateToken(String email, Long userId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (expirationHours * 3600L * 1000L));

        return Jwts.builder()
                .subject(email.toLowerCase())
                .claim("userId", userId)
                .claim("role", role != null ? role : "ROLE_USER")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extract email (subject) from JWT token.
     */
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extract userId claim from JWT token.
     */
    public Long getUserIdFromToken(String token) {
        Object val = getClaims(token).get("userId");
        if (val instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /**
     * Extract role claim from JWT token.
     */
    public String getRoleFromToken(String token) {
        Object val = getClaims(token).get("role");
        return val != null ? val.toString() : "ROLE_USER";
    }

    /**
     * Validate JWT token structure and signature.
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
