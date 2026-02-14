package com.sportstix.gateway.filter;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Shared utilities for gateway filters.
 * Gateway is WebFlux-based and cannot depend on common module.
 */
public final class RequestUtils {

    private RequestUtils() {}

    /**
     * Extract client IP from exchange.
     * Relies on Spring's forward-headers-strategy=framework to resolve
     * the correct remote address from trusted proxy headers.
     */
    public static String extractClientIp(ServerWebExchange exchange) {
        var remoteAddr = exchange.getRequest().getRemoteAddress();
        return remoteAddr != null ? remoteAddr.getAddress().getHostAddress() : "unknown";
    }

    /**
     * Write a JSON error response with proper escaping.
     */
    public static Mono<Void> writeJsonResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String escapedMessage = escapeJson(message);
        String escapedError = escapeJson(status.getReasonPhrase());
        String body = String.format(
                "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                status.value(), escapedError, escapedMessage);
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
