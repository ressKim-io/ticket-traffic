package com.sportstix.common.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingEvent extends DomainEvent {

    public static final String TYPE_CREATED = "BOOKING_CREATED";
    public static final String TYPE_CONFIRMED = "BOOKING_CONFIRMED";
    public static final String TYPE_CANCELLED = "BOOKING_CANCELLED";

    private Long bookingId;
    private Long userId;
    private Long gameId;
    private Long seatId;
    private BigDecimal totalPrice;

    private BookingEvent(String eventType, Long bookingId, Long userId,
                         Long gameId, Long seatId, BigDecimal totalPrice) {
        super(eventType);
        this.bookingId = bookingId;
        this.userId = userId;
        this.gameId = gameId;
        this.seatId = seatId;
        this.totalPrice = totalPrice;
    }

    public static BookingEvent created(Long bookingId, Long userId,
                                       Long gameId, Long seatId, BigDecimal totalPrice) {
        return new BookingEvent(TYPE_CREATED, bookingId, userId, gameId, seatId, totalPrice);
    }

    public static BookingEvent confirmed(Long bookingId, Long userId, Long gameId, Long seatId) {
        return new BookingEvent(TYPE_CONFIRMED, bookingId, userId, gameId, seatId, null);
    }

    public static BookingEvent cancelled(Long bookingId, Long userId, Long gameId, Long seatId) {
        return new BookingEvent(TYPE_CANCELLED, bookingId, userId, gameId, seatId, null);
    }
}
