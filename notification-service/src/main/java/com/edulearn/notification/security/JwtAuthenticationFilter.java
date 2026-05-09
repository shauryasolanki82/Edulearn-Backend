package com.edulearn.notification.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component @RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader("Authorization");
        String token = (StringUtils.hasText(h) && h.startsWith("Bearer ")) ? h.substring(7) : null;
        if (token != null && jwtUtil.validateToken(token)) {
            var auth = new UsernamePasswordAuthenticationToken(
                    jwtUtil.extractEmail(token), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + jwtUtil.extractRole(token))));
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            req.setAttribute("userId", jwtUtil.extractUserId(token));
            req.setAttribute("role",   jwtUtil.extractRole(token));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(req, res);
    }
}
