package com.sportstix.admin.event.consumer;

import com.sportstix.admin.service.AdminStatsService;
import com.sportstix.common.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminEventConsumer {

    private final AdminStatsService statsService;

    @KafkaListener(topics = Topics.BOOKING_CREATED, groupId = "admin-service")
    public void onBookingCreated(BookingEvent event) {
        statsService.processBookingCreated(event);
    }

    @KafkaListener(topics = Topics.BOOKING_CONFIRMED, groupId = "admin-service")
    public void onBookingConfirmed(BookingEvent event) {
        statsService.processBookingConfirmed(event);
    }

    @KafkaListener(topics = Topics.BOOKING_CANCELLED, groupId = "admin-service")
    public void onBookingCancelled(BookingEvent event) {
        statsService.processBookingCancelled(event);
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED, groupId = "admin-service")
    public void onPaymentCompleted(PaymentEvent event) {
        statsService.processPaymentCompleted(event);
    }

    @KafkaListener(topics = Topics.PAYMENT_REFUNDED, groupId = "admin-service")
    public void onPaymentRefunded(PaymentEvent event) {
        statsService.processPaymentRefunded(event);
    }

    @KafkaListener(topics = Topics.GAME_INFO_UPDATED, groupId = "admin-service")
    public void onGameInfoUpdated(GameInfoUpdatedEvent event) {
        statsService.processGameInfoUpdated(event);
    }
}
