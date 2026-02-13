package com.sportstix.queue.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitingRoomService {

    private static final String WAITING_ROOM_KEY_PREFIX = "waitingroom:";
    private static final String QUEUE_KEY_PREFIX = "queue:";

    private final StringRedisTemplate redisTemplate;

    public boolean register(Long gameId, Long userId) {
        String key = waitingRoomKey(gameId);
        Long added = redisTemplate.opsForSet().add(key, String.valueOf(userId));
        boolean isNew = added != null && added > 0;
        if (isNew) {
            log.info("User {} registered in waiting room for game {}", userId, gameId);
        }
        return isNew;
    }

    public boolean isRegistered(Long gameId, Long userId) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(waitingRoomKey(gameId), String.valueOf(userId))
        );
    }

    public Long getWaitingCount(Long gameId) {
        Long size = redisTemplate.opsForSet().size(waitingRoomKey(gameId));
        return size != null ? size : 0;
    }

    /**
     * Convert waiting room to queue at ticket open time.
     * Shuffles all registered users and inserts them into a Sorted Set with sequential scores.
     */
    public int convertToQueue(Long gameId) {
        String waitingKey = waitingRoomKey(gameId);
        String queueKey = QUEUE_KEY_PREFIX + gameId;

        Set<String> members = redisTemplate.opsForSet().members(waitingKey);
        if (members == null || members.isEmpty()) {
            log.info("No users in waiting room for game {}", gameId);
            return 0;
        }

        // Shuffle for fairness (Fisher-Yates via Collections.shuffle)
        List<String> shuffled = new ArrayList<>(members);
        Collections.shuffle(shuffled);

        // Batch ZADD with TypedTuple set (single round-trip)
        Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
        for (int i = 0; i < shuffled.size(); i++) {
            tuples.add(new DefaultTypedTuple<>(shuffled.get(i), (double) i));
        }
        redisTemplate.opsForZSet().add(queueKey, tuples);

        // Clean up waiting room
        redisTemplate.delete(waitingKey);

        log.info("Converted waiting room to queue for game {}: {} users shuffled", gameId, shuffled.size());
        return shuffled.size();
    }

    private String waitingRoomKey(Long gameId) {
        return WAITING_ROOM_KEY_PREFIX + gameId;
    }
}
