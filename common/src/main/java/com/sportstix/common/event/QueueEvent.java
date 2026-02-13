package com.sportstix.common.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QueueEvent extends DomainEvent {

    public static final String TYPE_ENTERED = "QUEUE_ENTERED";
    public static final String TYPE_TOKEN_ISSUED = "QUEUE_TOKEN_ISSUED";

    private Long gameId;
    private Long userId;
    private String token;

    private QueueEvent(String eventType, Long gameId, Long userId, String token) {
        super(eventType);
        this.gameId = gameId;
        this.userId = userId;
        this.token = token;
    }

    public static QueueEvent entered(Long gameId, Long userId) {
        return new QueueEvent(TYPE_ENTERED, gameId, userId, null);
    }

    public static QueueEvent tokenIssued(Long gameId, Long userId, String token) {
        return new QueueEvent(TYPE_TOKEN_ISSUED, gameId, userId, token);
    }
}
