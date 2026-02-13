package com.sportstix.queue.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks active WebSocket sessions per game on this pod.
 * Supports multiple sessions per user (e.g. multiple browser tabs)
 * via reference counting.
 */
@Slf4j
@Component
public class WebSocketSessionRegistry {

    // sessionId -> set of GameUser subscriptions
    private final Map<String, Set<GameUser>> sessionSubscriptions = new ConcurrentHashMap<>();

    // gameId -> (userId -> session ref count)
    private final Map<Long, ConcurrentHashMap<Long, AtomicInteger>> gameUserCounts = new ConcurrentHashMap<>();

    // userId -> set of sessionIds (for O(1) isConnected lookup)
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();

    public void registerSubscription(Long gameId, Long userId, String sessionId) {
        // Track session -> subscription mapping
        sessionSubscriptions.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(new GameUser(gameId, userId));

        // Increment ref count for gameId -> userId
        gameUserCounts.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(userId, k -> new AtomicInteger(0))
                .incrementAndGet();

        // Track userId -> sessionIds for O(1) isConnected
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);

        log.debug("Registered subscription: gameId={}, userId={}, sessionId={}", gameId, userId, sessionId);
    }

    public void removeSession(String sessionId) {
        Set<GameUser> subscriptions = sessionSubscriptions.remove(sessionId);
        if (subscriptions == null || subscriptions.isEmpty()) {
            return;
        }

        for (GameUser gu : subscriptions) {
            // Decrement ref count; remove userId entry if count reaches 0
            gameUserCounts.computeIfPresent(gu.gameId(), (gid, userCounts) -> {
                AtomicInteger count = userCounts.get(gu.userId());
                if (count != null && count.decrementAndGet() <= 0) {
                    userCounts.remove(gu.userId());
                }
                return userCounts.isEmpty() ? null : userCounts;
            });

            // Clean up userId -> sessionIds
            userSessions.computeIfPresent(gu.userId(), (uid, sessions) -> {
                sessions.remove(sessionId);
                return sessions.isEmpty() ? null : sessions;
            });
        }

        log.debug("Removed session: sessionId={}, subscriptions={}", sessionId, subscriptions.size());
    }

    public Set<Long> getSubscribedUsers(Long gameId) {
        ConcurrentHashMap<Long, AtomicInteger> userCounts = gameUserCounts.get(gameId);
        if (userCounts == null || userCounts.isEmpty()) {
            return Collections.emptySet();
        }
        return Set.copyOf(userCounts.keySet());
    }

    public int getConnectionCount(Long gameId) {
        ConcurrentHashMap<Long, AtomicInteger> userCounts = gameUserCounts.get(gameId);
        return userCounts != null ? userCounts.size() : 0;
    }

    public int getTotalConnections() {
        return sessionSubscriptions.size();
    }

    public boolean isConnected(Long userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    private record GameUser(Long gameId, Long userId) {}
}
