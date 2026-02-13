package com.sportstix.queue.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Actuator health indicator for WebSocket connections.
 * Reports total connections and per-game breakdown.
 */
@Component
@RequiredArgsConstructor
public class WebSocketHealthIndicator implements HealthIndicator {

    private static final int MAX_CONNECTIONS_THRESHOLD = 10_000;

    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public Health health() {
        int totalConnections = sessionRegistry.getTotalConnections();

        Health.Builder builder = totalConnections < MAX_CONNECTIONS_THRESHOLD
                ? Health.up()
                : Health.down().withDetail("reason", "Connection count exceeds threshold");

        return builder
                .withDetail("totalConnections", totalConnections)
                .build();
    }
}
