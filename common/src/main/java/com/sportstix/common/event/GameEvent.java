package com.sportstix.common.event;

import lombok.Getter;

@Getter
public class GameEvent extends DomainEvent {

    private final Long gameId;
    private final int totalSeats;

    private GameEvent(String eventType, Long gameId, int totalSeats) {
        super(eventType);
        this.gameId = gameId;
        this.totalSeats = totalSeats;
    }

    public static GameEvent seatInitialized(Long gameId, int totalSeats) {
        return new GameEvent("GAME_SEAT_INITIALIZED", gameId, totalSeats);
    }

    public static GameEvent infoUpdated(Long gameId) {
        return new GameEvent("GAME_INFO_UPDATED", gameId, 0);
    }
}
