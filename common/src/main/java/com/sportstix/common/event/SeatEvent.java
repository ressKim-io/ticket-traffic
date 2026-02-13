package com.sportstix.common.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatEvent extends DomainEvent {

    public static final String TYPE_HELD = "SEAT_HELD";
    public static final String TYPE_RELEASED = "SEAT_RELEASED";

    private Long gameId;
    private Long seatId;
    private Long userId;

    private SeatEvent(String eventType, Long gameId, Long seatId, Long userId) {
        super(eventType);
        this.gameId = gameId;
        this.seatId = seatId;
        this.userId = userId;
    }

    public static SeatEvent held(Long gameId, Long seatId, Long userId) {
        return new SeatEvent(TYPE_HELD, gameId, seatId, userId);
    }

    public static SeatEvent released(Long gameId, Long seatId, Long userId) {
        return new SeatEvent(TYPE_RELEASED, gameId, seatId, userId);
    }
}
