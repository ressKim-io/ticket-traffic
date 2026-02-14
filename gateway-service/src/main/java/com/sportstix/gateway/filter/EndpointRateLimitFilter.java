package com.sportstix.gateway.filter;

import com.sportstix.gateway.config.BotPreventionProperties;
import com.sportstix.gateway.config.BotPreventionProperties.EndpointLimit;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class EndpointRateLimitFilter implements GlobalFilter, Ordered {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    // Lua script: check limit before incrementing, atomic INCR + EXPIRE
    // ARGV[1] = windowSeconds, ARGV[2] = limit
    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = RedisScript.of(
            "local current = redis.call('GET', KEYS[1]) " +
            "if current and tonumber(current) >= tonumber(ARGV[2]) then " +
            "  return tonumber(current) + 1 " +
            "end " +
            "current = redis.call('INCR', KEYS[1]) " +
            "if current == 1 then redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1])) end " +
            "return current",
            Long.class);

    private final BotPreventionProperties properties;
    private final ReactiveStringRedisTemplate redisTemplate;

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
        String clientIp = RequestUtils.extractClientIp(exchange);
        String userId = exchange.getRequest().getHeaders().getFirst(HEADER_USER_ID);
        int windowSeconds = matchedLimit.getWindowSeconds();

        // Check IP limit first, then user limit only if IP is allowed
        String ipKey = String.format("rl:ep:%s:ip:%s", pathKey, clientIp);
        return checkRateLimit(ipKey, matchedLimit.getIpLimit(), windowSeconds)
                .flatMap(ipAllowed -> {
                    if (!ipAllowed) {
                        log.warn("IP rate limit exceeded for {} on path {}", clientIp, path);
                        return tooManyRequests(exchange, windowSeconds);
                    }
                    if (userId != null && !userId.isBlank()) {
                        String userKey = String.format("rl:ep:%s:user:%s", pathKey, userId);
                        return checkRateLimit(userKey, matchedLimit.getUserLimit(), windowSeconds)
                                .flatMap(userAllowed -> {
                                    if (!userAllowed) {
                                        log.warn("User rate limit exceeded for user {} on path {}",
                                                userId, path);
                                        return tooManyRequests(exchange, windowSeconds);
                                    }
                                    return chain.filter(exchange);
                                });
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
                        List.of(key),
                        List.of(String.valueOf(windowSeconds), String.valueOf(limit)))
                .next()
                .map(count -> count <= limit)
                .onErrorResume(e -> {
                    // Fail-open on Redis failure
                    log.error("Redis rate limit check failed, allowing request: {}", e.getMessage());
                    return Mono.just(true);
                });
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange, int retryAfterSeconds) {
        exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(retryAfterSeconds));
        return RequestUtils.writeJsonResponse(exchange, HttpStatus.TOO_MANY_REQUESTS,
                String.format("Rate limit exceeded. Retry after %d seconds", retryAfterSeconds));
    }
}
