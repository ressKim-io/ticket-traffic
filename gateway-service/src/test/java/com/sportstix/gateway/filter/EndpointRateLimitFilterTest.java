package com.sportstix.gateway.filter;

import com.sportstix.gateway.config.BotPreventionProperties;
import com.sportstix.gateway.config.BotPreventionProperties.EndpointLimit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EndpointRateLimitFilterTest {

    private BotPreventionProperties properties;
    private ReactiveStringRedisTemplate redisTemplate;
    private EndpointRateLimitFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        properties = new BotPreventionProperties();
        properties.getRateLimit().setEnabled(true);

        EndpointLimit queueLimit = new EndpointLimit();
        queueLimit.setPathPattern("/api/v1/queue/enter");
        queueLimit.setIpLimit(100);
        queueLimit.setUserLimit(10);
        queueLimit.setWindowSeconds(60);

        properties.getRateLimit().setEndpoints(Map.of("queue-enter", queueLimit));

        redisTemplate = mock(ReactiveStringRedisTemplate.class);
        filter = new EndpointRateLimitFilter(properties, redisTemplate);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void underLimit_shouldPassThrough() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyList()))
                .thenReturn(Flux.just(1L));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/queue/enter")
                        .header("X-User-Id", "user-1")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                        .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @SuppressWarnings("unchecked")
    @Test
    void ipLimitExceeded_shouldReturn429() {
        // IP limit exceeded (101 > 100), user limit OK
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyList()))
                .thenReturn(Flux.just(101L))  // IP check
                .thenReturn(Flux.just(1L));   // User check

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/queue/enter")
                        .header("X-User-Id", "user-1")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("60");
        verify(chain, never()).filter(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void userLimitExceeded_shouldReturn429() {
        // IP limit OK, user limit exceeded (11 > 10)
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyList()))
                .thenReturn(Flux.just(1L))    // IP check
                .thenReturn(Flux.just(11L));  // User check

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/queue/enter")
                        .header("X-User-Id", "user-1")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @SuppressWarnings("unchecked")
    @Test
    void redisFailure_shouldFailOpen() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyList()))
                .thenReturn(Flux.error(new RuntimeException("Redis down")));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/queue/enter")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                        .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
    }

    @Test
    void unmatchedPath_shouldPassThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/games")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                        .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void disabledRateLimit_shouldPassThrough() {
        properties.getRateLimit().setEnabled(false);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/queue/enter")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                        .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
        verifyNoInteractions(redisTemplate);
    }

    @SuppressWarnings("unchecked")
    @Test
    void noUserId_shouldOnlyCheckIpLimit() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyList()))
                .thenReturn(Flux.just(1L));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/queue/enter")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                        .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
        // Only 1 Redis call (IP only, no user)
        verify(redisTemplate, times(1)).execute(any(RedisScript.class), anyList(), anyList());
    }

    @Test
    void orderShouldBeMinusEighty() {
        assertThat(filter.getOrder()).isEqualTo(-80);
    }
}
