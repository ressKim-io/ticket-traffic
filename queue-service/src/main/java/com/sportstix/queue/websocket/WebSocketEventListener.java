package com.sportstix.queue.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;

/**
 * Listens for WebSocket lifecycle events to manage session tracking.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final WebSocketSessionRegistry sessionRegistry;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        log.info("WebSocket connected: sessionId={}, user={}",
                sessionId, accessor.getUser() != null ? accessor.getUser().getName() : "anonymous");
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();

        if (destination == null || sessionId == null) {
            return;
        }

        // Track user-specific queue subscriptions: /topic/queue/{gameId}/{userId}
        if (destination.matches("/topic/queue/\\d+/\\d+")) {
            Principal user = accessor.getUser();
            if (user == null) {
                log.warn("Unauthenticated subscription event for {}", destination);
                return;
            }

            try {
                String[] parts = destination.split("/");
                Long gameId = Long.parseLong(parts[3]);
                Long userId = Long.valueOf(user.getName());
                sessionRegistry.registerSubscription(gameId, userId, sessionId);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse gameId/userId from destination: {}", destination, e);
            }
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        if (sessionId != null) {
            sessionRegistry.removeSession(sessionId);
            log.info("WebSocket disconnected: sessionId={}", sessionId);
        }
    }
}
