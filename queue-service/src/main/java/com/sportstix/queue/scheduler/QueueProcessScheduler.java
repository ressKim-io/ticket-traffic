package com.sportstix.queue.scheduler;

import com.sportstix.queue.event.producer.QueueEventProducer;
import com.sportstix.queue.service.QueueService;
import com.sportstix.queue.service.TokenService;
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
    private final StringRedisTemplate redisTemplate;

    @Scheduled(fixedDelayString = "${queue.process-interval-ms:3000}")
    public void processQueues() {
        Set<String> activeGameIds = redisTemplate.opsForSet().members(ACTIVE_GAMES_KEY);
        if (activeGameIds == null || activeGameIds.isEmpty()) {
            return;
        }

        for (String gameIdStr : activeGameIds) {
            Long gameId = Long.parseLong(gameIdStr);
            processGameQueue(gameId);
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
            issued++;
        }

        Long remaining = queueService.getQueueSize(gameId);
        log.info("Game {} queue: issued {} tokens, {} remaining", gameId, issued, remaining);
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
