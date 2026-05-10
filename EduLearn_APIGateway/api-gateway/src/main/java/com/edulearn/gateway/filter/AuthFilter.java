package com.edulearn.gateway.filter;

import com.edulearn.gateway.config.AppProperties;
import com.edulearn.gateway.config.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * AuthFilter — named Gateway filter applied to every protected route.
 *
 * Behaviour:
 *  • OPTIONS requests pass through immediately (browser pre-flight).
 *  • Paths listed in app.public-endpoints bypass JWT validation.
 *  • All other requests must carry a valid Bearer token.
 *  • Validated claims (userId, email, role) are forwarded as X-* headers
 *    so downstream services can trust them without re-validating the token.
 */
@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AppProperties appProperties;

    public AuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 1. Let OPTIONS through for CORS pre-flight
            if (request.getMethod() == HttpMethod.OPTIONS) {
                return chain.filter(exchange);
            }

            String path = request.getURI().getPath();

            // 2. Skip JWT check for public endpoints
            if (isPublic(path)) {
                log.debug("Public endpoint, skipping auth: {}", path);
                return chain.filter(exchange);
            }

            // 3. Extract Authorization header
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or malformed Authorization header for path: {}", path);
                return unauthorized(exchange, "Authorization header missing or invalid");
            }

            String token = authHeader.substring(7);

            // 4. Validate JWT
            try {
                Claims claims = jwtUtil.validateAndGetClaims(token);

                // 5. Forward verified claims as request headers to downstream services
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id",    safeString(claims.get("userId")))
                        .header("X-User-Email", safeString(claims.getSubject()))
                        .header("X-User-Role",  safeString(claims.get("role")))
                        .build();

                log.debug("JWT valid for user={} role={} path={}",
                        claims.get("userId"), claims.get("role"), path);

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (Exception e) {
                log.warn("JWT validation failed for path {}: {}", path, e.getMessage());
                return unauthorized(exchange, "Invalid or expired token");
            }
        };
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private boolean isPublic(String path) {
        List<String> publicEndpoints = appProperties.getPublicEndpoints();
        if (publicEndpoints == null || publicEndpoints.isEmpty()) {
            return false;
        }
        return publicEndpoints.stream().anyMatch(ep -> {
            // Exact match or prefix match (e.g. /api/v1/courses matches /api/v1/courses)
            return path.equals(ep) || path.startsWith(ep + "/") || path.startsWith(ep + "?");
        });
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        var body = response.bufferFactory()
                .wrap(("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}").getBytes());
        return response.writeWith(Mono.just(body));
    }

    private String safeString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    public static class Config {
        // No config fields needed — behaviour controlled via application.yml
    }
}
