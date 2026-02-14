package com.sportstix.gateway.filter;

import com.sportstix.gateway.config.BotPreventionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotDetectionFilter implements GlobalFilter, Ordered {

    private final BotPreventionProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.getFingerprint().isEnabled()) {
            return chain.filter(exchange);
        }

        String userAgent = exchange.getRequest().getHeaders().getFirst(HttpHeaders.USER_AGENT);

        if (userAgent == null || userAgent.isBlank()) {
            log.warn("Blocked request with missing User-Agent from {}",
                    extractClientIp(exchange));
            return forbidden(exchange, "Missing User-Agent header");
        }

        for (String pattern : properties.getFingerprint().getBlockedUserAgentPatterns()) {
            if (Pattern.matches(pattern, userAgent)) {
                log.warn("Blocked bot User-Agent '{}' from {}",
                        userAgent, extractClientIp(exchange));
                return forbidden(exchange, "Blocked User-Agent");
            }
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -90;
    }

    private String extractClientIp(ServerWebExchange exchange) {
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        var remoteAddr = exchange.getRequest().getRemoteAddress();
        return remoteAddr != null ? remoteAddr.getAddress().getHostAddress() : "unknown";
    }

    static Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        return writeJsonResponse(exchange, HttpStatus.FORBIDDEN, message);
    }

    static Mono<Void> writeJsonResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format(
                "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                status.value(), status.getReasonPhrase(), message);
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
