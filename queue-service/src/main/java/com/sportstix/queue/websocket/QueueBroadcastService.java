package com.sportstix.queue.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportstix.queue.config.RedisPubSubConfig;
import com.sportstix.queue.dto.response.QueueUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes queue update messages to Redis Pub/Sub channel.
 * This enables multi-pod WebSocket broadcasting.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueBroadcastService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publish a queue update message to Redis Pub/Sub for cross-pod delivery.
     */
    public void broadcastUpdate(QueueUpdateMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(RedisPubSubConfig.QUEUE_UPDATE_CHANNEL, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize queue update message: {}", message, e);
        }
    }

}
