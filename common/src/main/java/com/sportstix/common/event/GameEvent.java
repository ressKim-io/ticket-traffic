package com.sportstix.common.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameEvent extends DomainEvent {

    public static final String TYPE_SEAT_INITIALIZED = "GAME_SEAT_INITIALIZED";
    public static final String TYPE_INFO_UPDATED = "GAME_INFO_UPDATED";

    private Long gameId;
    private Integer totalSeats;

    private GameEvent(String eventType, Long gameId, Integer totalSeats) {
        super(eventType);
        this.gameId = gameId;
        this.totalSeats = totalSeats;
    }

    public static GameEvent seatInitialized(Long gameId, int totalSeats) {
        return new GameEvent(TYPE_SEAT_INITIALIZED, gameId, totalSeats);
    }

    public static GameEvent infoUpdated(Long gameId) {
        return new GameEvent(TYPE_INFO_UPDATED, gameId, null);
    }
}
