package com.sportstix.common.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEventTest {

    @Test
    void bookingEvent_created_hasCorrectFields() {
        BookingEvent event = BookingEvent.created(1L, 100L, 200L, 300L);

        assertThat(event.getEventType()).isEqualTo("BOOKING_CREATED");
        assertThat(event.getBookingId()).isEqualTo(1L);
        assertThat(event.getUserId()).isEqualTo(100L);
        assertThat(event.getGameId()).isEqualTo(200L);
        assertThat(event.getSeatId()).isEqualTo(300L);
        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getOccurredAt()).isNotNull();
    }

    @Test
    void paymentEvent_completed_hasCorrectFields() {
        PaymentEvent event = PaymentEvent.completed(1L, 2L, 100L, BigDecimal.valueOf(50000));

        assertThat(event.getEventType()).isEqualTo("PAYMENT_COMPLETED");
        assertThat(event.getPaymentId()).isEqualTo(1L);
        assertThat(event.getBookingId()).isEqualTo(2L);
        assertThat(event.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
    }

    @Test
    void seatEvent_held_hasCorrectFields() {
        SeatEvent event = SeatEvent.held(200L, 300L, 100L);

        assertThat(event.getEventType()).isEqualTo("SEAT_HELD");
        assertThat(event.getGameId()).isEqualTo(200L);
        assertThat(event.getSeatId()).isEqualTo(300L);
        assertThat(event.getUserId()).isEqualTo(100L);
    }

    @Test
    void queueEvent_tokenIssued_hasToken() {
        QueueEvent event = QueueEvent.tokenIssued(200L, 100L, "abc-token");

        assertThat(event.getEventType()).isEqualTo("QUEUE_TOKEN_ISSUED");
        assertThat(event.getToken()).isEqualTo("abc-token");
    }

    @Test
    void gameEvent_seatInitialized_hasTotalSeats() {
        GameEvent event = GameEvent.seatInitialized(200L, 25000);

        assertThat(event.getEventType()).isEqualTo("GAME_SEAT_INITIALIZED");
        assertThat(event.getGameId()).isEqualTo(200L);
        assertThat(event.getTotalSeats()).isEqualTo(25000);
    }
}
