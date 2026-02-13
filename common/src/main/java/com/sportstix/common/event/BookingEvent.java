package com.sportstix.common.event;

import lombok.Getter;

@Getter
public class BookingEvent extends DomainEvent {

    private final Long bookingId;
    private final Long userId;
    private final Long gameId;
    private final Long seatId;

    private BookingEvent(String eventType, Long bookingId, Long userId, Long gameId, Long seatId) {
        super(eventType);
        this.bookingId = bookingId;
        this.userId = userId;
        this.gameId = gameId;
        this.seatId = seatId;
    }

    public static BookingEvent created(Long bookingId, Long userId, Long gameId, Long seatId) {
        return new BookingEvent("BOOKING_CREATED", bookingId, userId, gameId, seatId);
    }

    public static BookingEvent confirmed(Long bookingId, Long userId, Long gameId, Long seatId) {
        return new BookingEvent("BOOKING_CONFIRMED", bookingId, userId, gameId, seatId);
    }

    public static BookingEvent cancelled(Long bookingId, Long userId, Long gameId, Long seatId) {
        return new BookingEvent("BOOKING_CANCELLED", bookingId, userId, gameId, seatId);
    }
}
