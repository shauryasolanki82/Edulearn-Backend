package com.edulearn.enrollment.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;

@Component @Slf4j
public class JwtUtil {
    @Value("${app.jwt.secret}") private String jwtSecret;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));
    }

    public boolean validateToken(String token) {
        try { parseClaims(token); return true; }
        catch (JwtException | IllegalArgumentException e) { log.warn("Invalid JWT: {}", e.getMessage()); return false; }
    }

    public String extractEmail(String token)  { return parseClaims(token).getSubject(); }
    public String extractRole(String token)   { return (String) parseClaims(token).get("role"); }
    public Long   extractUserId(String token) {
        Object id = parseClaims(token).get("userId");
        if (id instanceof Integer i) return i.longValue();
        return (Long) id;
    }

    public String generateToken(Long userId, String email, String role) {
        return Jwts.builder()
            .setClaims(Map.of("userId", userId, "role", role))
            .setSubject(email).setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600000L))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
    }

    private Claims parseClaims(String t) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(t).getBody();
    }
}
