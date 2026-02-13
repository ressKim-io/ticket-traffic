package com.sportstix.queue.dto.response;

public record QueueStatusResponse(
        Long gameId,
        String status,
        Long rank,
        Long totalWaiting,
        Integer estimatedWaitSeconds,
        String token
) {
    public static QueueStatusResponse waiting(Long gameId, long rank, long totalWaiting, int estimatedWaitSeconds) {
        return new QueueStatusResponse(gameId, "WAITING", rank, totalWaiting, estimatedWaitSeconds, null);
    }

    public static QueueStatusResponse eligible(Long gameId, String token) {
        return new QueueStatusResponse(gameId, "ELIGIBLE", null, null, null, token);
    }
}
