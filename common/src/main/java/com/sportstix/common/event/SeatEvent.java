package com.sportstix.common.event;

import lombok.Getter;

@Getter
public class SeatEvent extends DomainEvent {

    private final Long gameId;
    private final Long seatId;
    private final Long userId;

    private SeatEvent(String eventType, Long gameId, Long seatId, Long userId) {
        super(eventType);
        this.gameId = gameId;
        this.seatId = seatId;
        this.userId = userId;
    }

    public static SeatEvent held(Long gameId, Long seatId, Long userId) {
        return new SeatEvent("SEAT_HELD", gameId, seatId, userId);
    }

    public static SeatEvent released(Long gameId, Long seatId, Long userId) {
        return new SeatEvent("SEAT_RELEASED", gameId, seatId, userId);
    }
}
