package com.sportstix.queue.scheduler;

import com.sportstix.queue.config.QueueProperties;
import com.sportstix.queue.dto.response.QueueUpdateMessage;
import com.sportstix.queue.event.producer.QueueEventProducer;
import com.sportstix.queue.service.QueueService;
import com.sportstix.queue.service.TokenService;
import com.sportstix.queue.websocket.QueueBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Periodically processes the queue by popping batches of users,
 * issuing entrance tokens, and publishing Kafka events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueueProcessScheduler {

    private static final String ACTIVE_GAMES_KEY = "queue:active-games";

    private final QueueService queueService;
    private final TokenService tokenService;
    private final QueueEventProducer queueEventProducer;
    private final QueueBroadcastService broadcastService;
    private final QueueProperties queueProperties;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(fixedDelayString = "${queue.process-interval-ms:3000}")
    public void processQueues() {
        Set<String> activeGameIds = redisTemplate.opsForSet().members(ACTIVE_GAMES_KEY);
        if (activeGameIds == null || activeGameIds.isEmpty()) {
            return;
        }

        for (String gameIdStr : activeGameIds) {
            try {
                Long gameId = Long.parseLong(gameIdStr);
                processGameQueue(gameId);
            } catch (Exception e) {
                log.error("Failed to process queue for game {}: {}", gameIdStr, e.getMessage(), e);
            }
        }
    }

    private void processGameQueue(Long gameId) {
        Set<String> batch = queueService.popNextBatch(gameId);
        if (batch == null || batch.isEmpty()) {
            return;
        }

        int issued = 0;
        for (String userIdStr : batch) {
            Long userId = Long.parseLong(userIdStr);
            String token = tokenService.issueToken(gameId, userId);
            queueService.addToActive(gameId, userId);
            queueEventProducer.publishTokenIssued(gameId, userId, token);

            // Notify user via WebSocket that they are now eligible
            broadcastService.broadcastUpdate(QueueUpdateMessage.eligible(gameId, userId, token));
            issued++;
        }

        Long remaining = queueService.getQueueSize(gameId);
        log.info("Game {} queue: issued {} tokens, {} remaining", gameId, issued, remaining);

        // Broadcast updated positions to remaining users
        broadcastRemainingPositions(gameId);
    }

    private void broadcastRemainingPositions(Long gameId) {
        Long total = queueService.getQueueSize(gameId);
        if (total == null || total == 0) {
            return;
        }

        Set<String> remaining = queueService.peekBatch(gameId, Math.min(total, 1000));
        if (remaining == null || remaining.isEmpty()) {
            return;
        }

        long rank = 1;
        int batchSize = queueProperties.getBatchSize();
        long intervalMs = queueProperties.getProcessIntervalMs();

        for (String userIdStr : remaining) {
            Long userId = Long.parseLong(userIdStr);
            long batchesAhead = rank / batchSize;
            int waitSeconds = (int) (batchesAhead * intervalMs / 1000);

            broadcastService.broadcastUpdate(
                    QueueUpdateMessage.waiting(gameId, userId, rank, total, waitSeconds)
            );
            rank++;
        }
    }

    public void activateGame(Long gameId) {
        redisTemplate.opsForSet().add(ACTIVE_GAMES_KEY, String.valueOf(gameId));
        log.info("Activated queue processing for game {}", gameId);
    }

    public void deactivateGame(Long gameId) {
        redisTemplate.opsForSet().remove(ACTIVE_GAMES_KEY, String.valueOf(gameId));
        log.info("Deactivated queue processing for game {}", gameId);
    }
}
