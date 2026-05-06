package com.edulearn.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // 256-bit Base64-encoded test secret
    private static final String TEST_SECRET =
            "dGVzdFNlY3JldEtleUZvckpXVFRlc3RpbmdQdXJwb3Nlc09ubHk=";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 3600000L);
    }

    @Test
    @DisplayName("generateToken — produces a non-blank token")
    void generateToken_notBlank() {
        String token = jwtUtil.generateToken(1L, "alice@test.com", "STUDENT");
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("extractEmail — returns correct email")
    void extractEmail() {
        String token = jwtUtil.generateToken(1L, "alice@test.com", "STUDENT");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("alice@test.com");
    }

    @Test
    @DisplayName("extractUserId — returns correct userId")
    void extractUserId() {
        String token = jwtUtil.generateToken(42L, "bob@test.com", "INSTRUCTOR");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("extractRole — returns correct role")
    void extractRole() {
        String token = jwtUtil.generateToken(1L, "admin@test.com", "ADMIN");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("validateToken — valid token returns true")
    void validateToken_valid() {
        String token = jwtUtil.generateToken(1L, "test@test.com", "STUDENT");
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken — tampered token returns false")
    void validateToken_tampered() {
        String token = jwtUtil.generateToken(1L, "test@test.com", "STUDENT");
        String tampered = token + "corrupted";
        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("validateToken — expired token returns false")
    void validateToken_expired() {
        // Use 1ms expiry so it expires immediately
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 1L);
        String token = jwtUtil.generateToken(1L, "test@test.com", "STUDENT");

        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("validateToken — blank/null input returns false")
    void validateToken_blank() {
        assertThat(jwtUtil.validateToken("")).isFalse();
        assertThat(jwtUtil.validateToken("garbage")).isFalse();
    }

    @Test
    @DisplayName("getExpirationMs — returns configured value")
    void getExpirationMs() {
        assertThat(jwtUtil.getExpirationMs()).isEqualTo(3600000L);
    }
}
