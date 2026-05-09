package com.edulearn.notification.security;

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

    private Key key() { return Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret)); }

    public boolean validateToken(String t) {
        try { claims(t); return true; }
        catch (JwtException | IllegalArgumentException e) { log.warn("JWT: {}", e.getMessage()); return false; }
    }

    public String extractEmail(String t)  { return claims(t).getSubject(); }
    public String extractRole(String t)   { return (String) claims(t).get("role"); }
    public Long   extractUserId(String t) {
        Object id = claims(t).get("userId");
        if (id instanceof Integer i) return i.longValue();
        return (Long) id;
    }

    public String generateToken(Long uid, String email, String role) {
        return Jwts.builder()
                .setClaims(Map.of("userId", uid, "role", role))
                .setSubject(email).setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000L))
                .signWith(key(), SignatureAlgorithm.HS256).compact();
    }

    private Claims claims(String t) {
        return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(t).getBody();
    }
}
