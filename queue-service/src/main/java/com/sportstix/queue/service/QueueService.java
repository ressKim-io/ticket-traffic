package com.sportstix.queue.service;

import com.sportstix.common.exception.BusinessException;
import com.sportstix.common.response.ErrorCode;
import com.sportstix.queue.config.QueueProperties;
import com.sportstix.queue.dto.response.QueueStatusResponse;
import com.sportstix.queue.event.producer.QueueEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private static final String QUEUE_KEY_PREFIX = "queue:";
    private static final String ACTIVE_KEY_PREFIX = "queue:active:";

    private final StringRedisTemplate redisTemplate;
    private final TokenService tokenService;
    private final QueueEventProducer queueEventProducer;
    private final QueueProperties queueProperties;

    public QueueStatusResponse enterQueue(Long gameId, Long userId) {
        String queueKey = queueKey(gameId);
        String userIdStr = String.valueOf(userId);

        // Check if already in queue
        Double existingScore = redisTemplate.opsForZSet().score(queueKey, userIdStr);
        if (existingScore != null) {
            return getQueueStatus(gameId, userId);
        }

        // Check if already has token (already eligible)
        if (tokenService.hasToken(gameId, userId)) {
            String token = tokenService.getToken(gameId, userId);
            return QueueStatusResponse.eligible(gameId, token);
        }

        // Add to sorted set with timestamp as score
        double score = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(queueKey, userIdStr, score);

        Long rank = redisTemplate.opsForZSet().rank(queueKey, userIdStr);
        Long totalWaiting = redisTemplate.opsForZSet().size(queueKey);

        queueEventProducer.publishEntered(gameId, userId);

        log.info("User {} entered queue for game {}, rank={}", userId, gameId, rank);

        return QueueStatusResponse.waiting(
                gameId,
                rank != null ? rank + 1 : 1,
                totalWaiting != null ? totalWaiting : 1,
                estimateWaitSeconds(rank != null ? rank + 1 : 1)
        );
    }

    public QueueStatusResponse getQueueStatus(Long gameId, Long userId) {
        // Check if has entrance token
        if (tokenService.hasToken(gameId, userId)) {
            String token = tokenService.getToken(gameId, userId);
            return QueueStatusResponse.eligible(gameId, token);
        }

        String queueKey = queueKey(gameId);
        String userIdStr = String.valueOf(userId);

        Long rank = redisTemplate.opsForZSet().rank(queueKey, userIdStr);
        if (rank == null) {
            throw new BusinessException(ErrorCode.QUEUE_NOT_OPEN, "User not in queue for game: " + gameId);
        }

        Long totalWaiting = redisTemplate.opsForZSet().size(queueKey);

        return QueueStatusResponse.waiting(
                gameId,
                rank + 1,
                totalWaiting != null ? totalWaiting : 0,
                estimateWaitSeconds(rank + 1)
        );
    }

    public void leaveQueue(Long gameId, Long userId) {
        String queueKey = queueKey(gameId);
        redisTemplate.opsForZSet().remove(queueKey, String.valueOf(userId));
        tokenService.revokeToken(gameId, userId);
        log.info("User {} left queue for game {}", userId, gameId);
    }

    public Set<String> popNextBatch(Long gameId) {
        String queueKey = queueKey(gameId);
        int batchSize = queueProperties.getBatchSize();

        Set<String> batch = redisTemplate.opsForZSet().range(queueKey, 0, batchSize - 1);
        if (batch != null && !batch.isEmpty()) {
            redisTemplate.opsForZSet().remove(queueKey, batch.toArray());
        }
        return batch;
    }

    public Long getQueueSize(Long gameId) {
        Long size = redisTemplate.opsForZSet().size(queueKey(gameId));
        return size != null ? size : 0;
    }

    public void addToActive(Long gameId, Long userId) {
        redisTemplate.opsForSet().add(activeKey(gameId), String.valueOf(userId));
    }

    public boolean isActive(Long gameId, Long userId) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(activeKey(gameId), String.valueOf(userId))
        );
    }

    private int estimateWaitSeconds(long rank) {
        int batchSize = queueProperties.getBatchSize();
        long intervalMs = queueProperties.getProcessIntervalMs();
        long batchesAhead = rank / batchSize;
        return (int) (batchesAhead * intervalMs / 1000);
    }

    private String queueKey(Long gameId) {
        return QUEUE_KEY_PREFIX + gameId;
    }

    private String activeKey(Long gameId) {
        return ACTIVE_KEY_PREFIX + gameId;
    }
}
