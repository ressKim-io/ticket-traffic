package com.sportstix.queue.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active WebSocket sessions per game on this pod.
 * Used to efficiently determine which users are connected locally
 * and need to receive broadcasts.
 *
 * Key structure:
 * - gameId -> Set of userIds connected on this pod
 */
@Slf4j
@Component
public class WebSocketSessionRegistry {

    // gameId -> set of userIds subscribed on this pod
    private final Map<Long, Set<Long>> gameSubscriptions = new ConcurrentHashMap<>();

    // userId -> sessionId for disconnect tracking
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    public void registerSubscription(Long gameId, Long userId, String sessionId) {
        gameSubscriptions.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet())
                .add(userId);
        sessionUserMap.put(sessionId, userId);
        log.debug("Registered subscription: gameId={}, userId={}, sessionId={}", gameId, userId, sessionId);
    }

    public void removeSession(String sessionId) {
        Long userId = sessionUserMap.remove(sessionId);
        if (userId == null) {
            return;
        }

        // Remove from all game subscriptions
        gameSubscriptions.forEach((gameId, users) -> {
            if (users.remove(userId)) {
                log.debug("Removed subscription: gameId={}, userId={}, sessionId={}", gameId, userId, sessionId);
                if (users.isEmpty()) {
                    gameSubscriptions.remove(gameId);
                }
            }
        });
    }

    public Set<Long> getSubscribedUsers(Long gameId) {
        return gameSubscriptions.getOrDefault(gameId, Collections.emptySet());
    }

    public int getConnectionCount(Long gameId) {
        return gameSubscriptions.getOrDefault(gameId, Collections.emptySet()).size();
    }

    public int getTotalConnections() {
        return sessionUserMap.size();
    }

    public boolean isConnected(Long userId) {
        return sessionUserMap.containsValue(userId);
    }
}
