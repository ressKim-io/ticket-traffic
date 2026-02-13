package com.sportstix.queue.websocket;

import com.sportstix.queue.dto.response.QueueStatusResponse;
import com.sportstix.queue.dto.response.QueueUpdateMessage;
import com.sportstix.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
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
            org.springframework.messaging.simp.stomp.StompHeaderAccessor headerAccessor
    ) {
        String userIdHeader = headerAccessor.getFirstNativeHeader("X-User-Id");
        if (userIdHeader == null) {
            log.warn("Missing X-User-Id header in WebSocket status request");
            return null;
        }

        Long userId = Long.parseLong(userIdHeader);
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
    }
}
