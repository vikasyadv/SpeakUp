package com.speakup.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private static final String SECRET_A = "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256-key-a";
    private static final String SECRET_B = "different-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256-b";

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRET_A, 24);
    }

    @Test
    void generateToken_withPrincipal_generatesParsableAndValidToken() {
        UserPrincipal principal = new UserPrincipal(
                42L,
                "jane@example.com",
                "hashedpassword",
                "Jane Doe",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        String token = tokenProvider.generateToken(principal);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(tokenProvider.validateToken(token));
        assertEquals("jane@example.com", tokenProvider.getEmailFromToken(token));
        assertEquals(42L, tokenProvider.getUserIdFromToken(token));
        assertEquals("ROLE_USER", tokenProvider.getRoleFromToken(token));
    }

    @Test
    void generateToken_withExplicitArgs_generatesValidToken() {
        String token = tokenProvider.generateToken("alex@example.com", 99L, "ROLE_ADMIN");

        assertTrue(tokenProvider.validateToken(token));
        assertEquals("alex@example.com", tokenProvider.getEmailFromToken(token));
        assertEquals(99L, tokenProvider.getUserIdFromToken(token));
        assertEquals("ROLE_ADMIN", tokenProvider.getRoleFromToken(token));
    }

    @Test
    void validateToken_invalidSignature_returnsFalse() {
        // Generate token with SECRET_B
        JwtTokenProvider otherProvider = new JwtTokenProvider(SECRET_B, 24);
        String tokenFromOther = otherProvider.generateToken("imposter@example.com", 1L, "ROLE_USER");

        // Validate using tokenProvider (which uses SECRET_A)
        assertFalse(tokenProvider.validateToken(tokenFromOther));
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        // Expiration in negative hours (-1)
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET_A, -1);
        String expiredToken = expiredProvider.generateToken("expired@example.com", 5L, "ROLE_USER");

        assertFalse(tokenProvider.validateToken(expiredToken));
    }

    @Test
    void validateToken_malformedToken_returnsFalse() {
        assertFalse(tokenProvider.validateToken("not-a-valid-jwt-token"));
        assertFalse(tokenProvider.validateToken(""));
        assertFalse(tokenProvider.validateToken(null));
        assertFalse(tokenProvider.validateToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.badpayload.badsig"));
    }
}
