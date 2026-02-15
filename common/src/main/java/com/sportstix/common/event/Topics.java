package com.sportstix.common.event;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Topics {

    // Queue
    public static final String QUEUE_ENTERED = "ticket.queue.entered";
    public static final String QUEUE_TOKEN_ISSUED = "ticket.queue.token-issued";

    // Booking
    public static final String BOOKING_CREATED = "ticket.booking.created";
    public static final String BOOKING_CONFIRMED = "ticket.booking.confirmed";
    public static final String BOOKING_CANCELLED = "ticket.booking.cancelled";

    // Seat
    public static final String SEAT_HELD = "ticket.seat.held";
    public static final String SEAT_RELEASED = "ticket.seat.released";

    // Payment
    public static final String PAYMENT_COMPLETED = "ticket.payment.completed";
    public static final String PAYMENT_FAILED = "ticket.payment.failed";
    public static final String PAYMENT_REFUNDED = "ticket.payment.refunded";

    // Game
    public static final String GAME_SEAT_INITIALIZED = "ticket.game.seat-initialized";
    public static final String GAME_INFO_UPDATED = "ticket.game.info-updated";

    // Dead Letter Topics (DLT) - suffix: .DLT
    public static final String DLT_SUFFIX = ".DLT";

    // Partition counts per topic category
    public static final int PARTITIONS_BOOKING = 8;
    public static final int PARTITIONS_SEAT = 8;
    public static final int PARTITIONS_PAYMENT = 6;
    public static final int PARTITIONS_QUEUE = 4;
    public static final int PARTITIONS_GAME = 3;

    public static String dlt(String topic) {
        return topic + DLT_SUFFIX;
    }
}
