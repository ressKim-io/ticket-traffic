package com.sportstix.queue.websocket;

import com.sportstix.queue.dto.response.QueueStatusResponse;
import com.sportstix.queue.dto.response.QueueUpdateMessage;
import com.sportstix.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * STOMP controller for queue-related WebSocket messages.
 * Clients can request their current status via STOMP messaging.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class QueueWebSocketController {

    private final QueueService queueService;

    /**
     * Client sends to /app/queue/status/{gameId} with userId in header.
     * Response sent to /user/topic/queue/status.
     */
    @MessageMapping("/queue/status/{gameId}")
    @SendToUser("/topic/queue/status")
    public QueueUpdateMessage getStatus(
            @DestinationVariable Long gameId,
            StompHeaderAccessor headerAccessor
    ) {
        String userIdHeader = headerAccessor.getFirstNativeHeader("X-User-Id");
        if (userIdHeader == null) {
            log.warn("Missing X-User-Id header in WebSocket status request");
            return QueueUpdateMessage.error(gameId, "Missing X-User-Id header");
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            log.warn("Invalid X-User-Id header: {}", userIdHeader);
            return QueueUpdateMessage.error(gameId, "Invalid X-User-Id header");
        }

        try {
            QueueStatusResponse status = queueService.getQueueStatus(gameId, userId);
            return new QueueUpdateMessage(
                    status.gameId(),
                    userId,
                    status.status(),
                    status.rank(),
                    status.totalWaiting(),
                    status.estimatedWaitSeconds(),
                    status.token()
            );
        } catch (Exception e) {
            log.warn("Failed to get queue status: gameId={}, userId={}", gameId, userId, e);
            return QueueUpdateMessage.error(gameId, "Not in queue");
        }
    }
}
