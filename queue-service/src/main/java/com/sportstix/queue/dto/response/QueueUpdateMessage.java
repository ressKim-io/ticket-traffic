package com.sportstix.queue.dto.response;

/**
 * WebSocket message sent to individual users with their queue position update.
 */
public record QueueUpdateMessage(
        Long gameId,
        Long userId,
        String status,
        Long rank,
        Long totalWaiting,
        Integer estimatedWaitSeconds,
        String token
) {
    public static QueueUpdateMessage waiting(Long gameId, Long userId, long rank, long totalWaiting, int estimatedWaitSeconds) {
        return new QueueUpdateMessage(gameId, userId, "WAITING", rank, totalWaiting, estimatedWaitSeconds, null);
    }

    public static QueueUpdateMessage eligible(Long gameId, Long userId, String token) {
        return new QueueUpdateMessage(gameId, userId, "ELIGIBLE", null, null, null, token);
    }

    public static QueueUpdateMessage completed(Long gameId, Long userId) {
        return new QueueUpdateMessage(gameId, userId, "COMPLETED", null, null, null, null);
    }

    public static QueueUpdateMessage error(Long gameId, String reason) {
        return new QueueUpdateMessage(gameId, null, "ERROR", null, null, null, reason);
    }
}
