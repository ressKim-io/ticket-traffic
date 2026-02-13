package com.sportstix.common.event;

import lombok.Getter;

@Getter
public class QueueEvent extends DomainEvent {

    private final Long gameId;
    private final Long userId;
    private final String token;

    private QueueEvent(String eventType, Long gameId, Long userId, String token) {
        super(eventType);
        this.gameId = gameId;
        this.userId = userId;
        this.token = token;
    }

    public static QueueEvent entered(Long gameId, Long userId) {
        return new QueueEvent("QUEUE_ENTERED", gameId, userId, null);
    }

    public static QueueEvent tokenIssued(Long gameId, Long userId, String token) {
        return new QueueEvent("QUEUE_TOKEN_ISSUED", gameId, userId, token);
    }
}
