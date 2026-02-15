package com.sportstix.booking.event.outbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventTest {

    @Test
    void markPublished_setsStatusAndTimestamp() {
        OutboxEvent event = new OutboxEvent("Booking", "1",
                "BOOKING_CREATED", "topic", "key", "{}");

        event.markPublished();

        assertThat(event.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    void markRetrying_incrementsRetryCount() {
        OutboxEvent event = new OutboxEvent("Booking", "1",
                "BOOKING_CREATED", "topic", "key", "{}");

        event.markRetrying();
        event.markRetrying();

        assertThat(event.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.RETRYING);
        assertThat(event.getRetryCount()).isEqualTo(2);
    }

    @Test
    void markFailed_setsFailedStatus() {
        OutboxEvent event = new OutboxEvent("Booking", "1",
                "BOOKING_CREATED", "topic", "key", "{}");

        event.markFailed();

        assertThat(event.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.FAILED);
    }
}
