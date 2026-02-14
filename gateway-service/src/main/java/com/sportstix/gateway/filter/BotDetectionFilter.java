package com.sportstix.gateway.filter;

import com.sportstix.gateway.config.BotPreventionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class BotDetectionFilter implements GlobalFilter, Ordered {

    private final BotPreventionProperties properties;
    private final List<String> lowerCasePatterns;

    public BotDetectionFilter(BotPreventionProperties properties) {
        this.properties = properties;
        // Pre-compute lowercase patterns for case-insensitive substring matching.
        // Using contains() instead of regex eliminates ReDoS risk entirely.
        this.lowerCasePatterns = properties.getFingerprint().getBlockedUserAgentPatterns()
                .stream()
                .map(String::toLowerCase)
                .toList();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.getFingerprint().isEnabled()) {
            return chain.filter(exchange);
        }

        String userAgent = exchange.getRequest().getHeaders().getFirst(HttpHeaders.USER_AGENT);

        if (userAgent == null || userAgent.isBlank()) {
            log.warn("Blocked request with missing User-Agent from {}",
                    RequestUtils.extractClientIp(exchange));
            return RequestUtils.writeJsonResponse(exchange, HttpStatus.FORBIDDEN,
                    "Missing User-Agent header");
        }

        String lowerUa = userAgent.toLowerCase();
        for (String pattern : lowerCasePatterns) {
            if (lowerUa.contains(pattern)) {
                String sanitized = userAgent.replaceAll("[\\r\\n]", "_");
                log.warn("Blocked bot User-Agent '{}' from {}",
                        sanitized, RequestUtils.extractClientIp(exchange));
                return RequestUtils.writeJsonResponse(exchange, HttpStatus.FORBIDDEN,
                        "Blocked User-Agent");
            }
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -90;
    }
}
