package com.sportstix.queue.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportstix.queue.dto.response.QueueUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Subscribes to Redis Pub/Sub and forwards messages to STOMP WebSocket clients.
 * Each pod receives the same message and delivers to locally connected clients.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisQueueMessageSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Called by Redis MessageListenerAdapter when a message arrives.
     */
    public void onMessage(String message) {
        try {
            QueueUpdateMessage update = objectMapper.readValue(message, QueueUpdateMessage.class);

            // Send to user-specific topic: /topic/queue/{gameId}/{userId}
            String destination = String.format("/topic/queue/%d/%d", update.gameId(), update.userId());
            messagingTemplate.convertAndSend(destination, update);

            // Also send to game-level topic for admin/monitoring
            String gameTopic = String.format("/topic/queue/%d", update.gameId());
            messagingTemplate.convertAndSend(gameTopic, update);

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize queue update message: {}", message, e);
        }
    }
}
