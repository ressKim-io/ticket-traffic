package com.sportstix.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final JwtParser jwtParser;
    private final List<String> publicPaths;

    public JwtAuthFilter(
            @Value("${gateway.jwt.public-key}") String publicKeyPem,
            @Value("${gateway.jwt.public-paths}") List<String> publicPaths) {
        this.jwtParser = Jwts.parser()
                .verifyWith(parsePublicKey(publicKeyPem))
                .build();
        this.publicPaths = publicPaths;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            // Strip any spoofed identity headers on public paths
            ServerHttpRequest cleaned = exchange.getRequest().mutate()
                    .headers(h -> {
                        h.remove(HEADER_USER_ID);
                        h.remove(HEADER_USER_ROLE);
                    })
                    .build();
            return chain.filter(exchange.mutate().request(cleaned).build());
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();

            String role = claims.get("role", String.class);
            if (role == null) {
                log.warn("JWT missing required 'role' claim for subject: {}", claims.getSubject());
                return unauthorized(exchange);
            }

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .headers(h -> {
                        h.remove(HEADER_USER_ID);
                        h.remove(HEADER_USER_ROLE);
                    })
                    .header(HEADER_USER_ID, claims.getSubject())
                    .header(HEADER_USER_ROLE, role)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return unauthorized(exchange);
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isPublicPath(String path) {
        return publicPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    // Intentionally duplicated from auth-service: gateway is WebFlux and cannot depend on common module (servlet-based)
    private static PublicKey parsePublicKey(String pem) {
        try {
            String base64 = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(base64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RSA public key", e);
        }
    }
}
