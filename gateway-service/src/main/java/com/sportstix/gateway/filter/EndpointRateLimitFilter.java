package com.sportstix.gateway.filter;

import com.sportstix.gateway.config.BotPreventionProperties;
import com.sportstix.gateway.config.BotPreventionProperties.EndpointLimit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class EndpointRateLimitFilter implements GlobalFilter, Ordered {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    // Lua script: atomic INCR + EXPIRE, returns current count
    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = RedisScript.of(
            "local current = redis.call('INCR', KEYS[1]) " +
            "if current == 1 then redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1])) end " +
            "return current",
            Long.class);

    private final BotPreventionProperties properties;
    private final ReactiveStringRedisTemplate redisTemplate;

    public EndpointRateLimitFilter(BotPreventionProperties properties,
                                   ReactiveStringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.getRateLimit().isEnabled()) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();
        EndpointLimit matchedLimit = findMatchingEndpoint(path);
        if (matchedLimit == null) {
            return chain.filter(exchange);
        }

        String pathKey = matchedLimit.getPathPattern().replace("/", "_");
        String clientIp = extractClientIp(exchange);
        String userId = exchange.getRequest().getHeaders().getFirst(HEADER_USER_ID);
        int windowSeconds = matchedLimit.getWindowSeconds();

        // IP-based rate limit check
        String ipKey = String.format("rl:ep:%s:ip:%s", pathKey, clientIp);
        Mono<Boolean> ipCheck = checkRateLimit(ipKey, matchedLimit.getIpLimit(), windowSeconds);

        // User-based rate limit check (only if user is authenticated)
        Mono<Boolean> userCheck;
        if (userId != null && !userId.isBlank()) {
            String userKey = String.format("rl:ep:%s:user:%s", pathKey, userId);
            userCheck = checkRateLimit(userKey, matchedLimit.getUserLimit(), windowSeconds);
        } else {
            userCheck = Mono.just(true);
        }

        return Mono.zip(ipCheck, userCheck)
                .flatMap(tuple -> {
                    boolean ipAllowed = tuple.getT1();
                    boolean userAllowed = tuple.getT2();

                    if (!ipAllowed) {
                        log.warn("IP rate limit exceeded for {} on path {}", clientIp, path);
                        return tooManyRequests(exchange, windowSeconds);
                    }
                    if (!userAllowed) {
                        log.warn("User rate limit exceeded for user {} on path {}", userId, path);
                        return tooManyRequests(exchange, windowSeconds);
                    }
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return -80;
    }

    private EndpointLimit findMatchingEndpoint(String path) {
        Map<String, EndpointLimit> endpoints = properties.getRateLimit().getEndpoints();
        for (EndpointLimit limit : endpoints.values()) {
            if (PATH_MATCHER.match(limit.getPathPattern(), path)) {
                return limit;
            }
        }
        return null;
    }

    private Mono<Boolean> checkRateLimit(String key, int limit, int windowSeconds) {
        return redisTemplate.execute(RATE_LIMIT_SCRIPT,
                        List.of(key), List.of(String.valueOf(windowSeconds)))
                .next()
                .map(count -> count <= limit)
                .onErrorResume(e -> {
                    // Fail-open on Redis failure
                    log.error("Redis rate limit check failed, allowing request: {}", e.getMessage());
                    return Mono.just(true);
                });
    }

    private String extractClientIp(ServerWebExchange exchange) {
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        var remoteAddr = exchange.getRequest().getRemoteAddress();
        return remoteAddr != null ? remoteAddr.getAddress().getHostAddress() : "unknown";
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange, int retryAfterSeconds) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(retryAfterSeconds));
        String body = String.format(
                "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Retry after %d seconds\"}",
                retryAfterSeconds);
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
