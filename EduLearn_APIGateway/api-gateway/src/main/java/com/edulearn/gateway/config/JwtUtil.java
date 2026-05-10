package com.edulearn.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;

@Component
public class JwtUtil {

    private final SecretKey key;

    @Autowired
    public JwtUtil(AppProperties appProperties) {
        String secret = appProperties.getJwt().getSecret();
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Validates the JWT and returns its claims.
     * Throws JwtException if invalid or expired.
     */
    public Claims validateAndGetClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Returns true if the token is structurally valid and not expired.
     */
    public boolean isValid(String token) {
        try {
            validateAndGetClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
