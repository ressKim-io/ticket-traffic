package com.sportstix.queue.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Validates WebSocket STOMP connections and subscriptions.
 * Ensures users can only subscribe to their own queue topics.
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        // Extract userId from STOMP CONNECT headers (set by gateway)
        String userIdHeader = accessor.getFirstNativeHeader("X-User-Id");
        if (userIdHeader != null) {
            try {
                Long userId = Long.parseLong(userIdHeader);
                accessor.setUser(new QueuePrincipal(userId));
                log.debug("WebSocket connected: userId={}", userId);
            } catch (NumberFormatException e) {
                log.warn("Invalid X-User-Id in CONNECT: {}", userIdHeader);
            }
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        // Allow game-level topic for all authenticated users
        // /topic/queue/{gameId} - public stats
        if (destination.matches("/topic/queue/\\d+$")) {
            return;
        }

        // User-specific topic: /topic/queue/{gameId}/{userId}
        // Verify the userId in destination matches the authenticated user
        if (destination.matches("/topic/queue/\\d+/\\d+")) {
            Principal user = accessor.getUser();
            if (user == null) {
                log.warn("Unauthenticated subscription attempt to {}", destination);
                throw new IllegalStateException("Authentication required");
            }

            String[] parts = destination.split("/");
            String targetUserId = parts[parts.length - 1];

            if (!targetUserId.equals(user.getName())) {
                log.warn("User {} attempted to subscribe to another user's topic: {}",
                        user.getName(), destination);
                throw new IllegalStateException("Cannot subscribe to another user's queue topic");
            }
        }
    }

    /**
     * Simple Principal implementation carrying the userId.
     */
    private record QueuePrincipal(Long userId) implements Principal {
        @Override
        public String getName() {
            return String.valueOf(userId);
        }
    }
}
