package com.sportstix.admin.event.consumer;

import com.sportstix.admin.service.AdminStatsService;
import com.sportstix.common.event.BookingEvent;
import com.sportstix.common.event.GameInfoUpdatedEvent;
import com.sportstix.common.event.PaymentEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminEventConsumerTest {

    @Mock
    private AdminStatsService statsService;

    @InjectMocks
    private AdminEventConsumer consumer;

    @Test
    @DisplayName("onBookingCreated delegates to statsService")
    void onBookingCreated_delegates() {
        BookingEvent event = BookingEvent.created(1L, 1L, 1L, 100L, new BigDecimal("50000"));
        consumer.onBookingCreated(event);
        verify(statsService).processBookingCreated(event);
    }

    @Test
    @DisplayName("onBookingConfirmed delegates to statsService")
    void onBookingConfirmed_delegates() {
        BookingEvent event = BookingEvent.confirmed(1L, 1L, 1L, 100L);
        consumer.onBookingConfirmed(event);
        verify(statsService).processBookingConfirmed(event);
    }

    @Test
    @DisplayName("onBookingCancelled delegates to statsService")
    void onBookingCancelled_delegates() {
        BookingEvent event = BookingEvent.cancelled(1L, 1L, 1L, 100L);
        consumer.onBookingCancelled(event);
        verify(statsService).processBookingCancelled(event);
    }

    @Test
    @DisplayName("onPaymentCompleted delegates to statsService")
    void onPaymentCompleted_delegates() {
        PaymentEvent event = PaymentEvent.completed(1L, 1L, 1L, new BigDecimal("50000"));
        consumer.onPaymentCompleted(event);
        verify(statsService).processPaymentCompleted(event);
    }

    @Test
    @DisplayName("onPaymentRefunded delegates to statsService")
    void onPaymentRefunded_delegates() {
        PaymentEvent event = PaymentEvent.refunded(1L, 1L, 1L, new BigDecimal("50000"));
        consumer.onPaymentRefunded(event);
        verify(statsService).processPaymentRefunded(event);
    }

    @Test
    @DisplayName("onGameInfoUpdated delegates to statsService")
    void onGameInfoUpdated_delegates() {
        GameInfoUpdatedEvent event = new GameInfoUpdatedEvent(
                1L, "Home", "Away", LocalDateTime.now(), LocalDateTime.now(), "OPEN", 4);
        consumer.onGameInfoUpdated(event);
        verify(statsService).processGameInfoUpdated(event);
    }
}
