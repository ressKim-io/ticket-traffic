package com.sportstix.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private static final String SECRET = "sportstix-default-secret-key-for-development-only-32bytes!";
    private JwtAuthFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(SECRET, List.of("/api/v1/auth/login", "/api/v1/auth/signup", "/actuator/**"));
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void publicPath_shouldPassThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/login").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void publicPath_shouldStripSpoofedHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/login")
                        .header("X-User-Id", "spoofed-id")
                        .header("X-User-Role", "ADMIN")
                        .build());

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        ServerWebExchange forwarded = captor.getValue();
        assertThat(forwarded.getRequest().getHeaders().getFirst("X-User-Id")).isNull();
        assertThat(forwarded.getRequest().getHeaders().getFirst("X-User-Role")).isNull();
    }

    @Test
    void missingToken_shouldReturn401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/games/1").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void invalidToken_shouldReturn401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/games/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validToken_shouldForwardUserHeaders() {
        String token = createToken("123", "USER", 60000);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/games/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        ServerWebExchange forwarded = captor.getValue();
        assertThat(forwarded.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("123");
        assertThat(forwarded.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("USER");
    }

    @Test
    void validToken_shouldStripExistingSpoofedHeaders() {
        String token = createToken("123", "USER", 60000);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/games/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", "spoofed-admin")
                        .header("X-User-Role", "ADMIN")
                        .build());

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        ServerWebExchange forwarded = captor.getValue();
        assertThat(forwarded.getRequest().getHeaders().get("X-User-Id")).containsExactly("123");
        assertThat(forwarded.getRequest().getHeaders().get("X-User-Role")).containsExactly("USER");
    }

    @Test
    void expiredToken_shouldReturn401() {
        String token = createToken("123", "USER", -60000);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/games/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenWithoutRole_shouldReturn401() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("123")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key)
                .compact();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/games/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void actuatorWildcard_shouldPassThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String createToken(String subject, String role, long expirationOffsetMs) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationOffsetMs))
                .signWith(key)
                .compact();
    }
}
