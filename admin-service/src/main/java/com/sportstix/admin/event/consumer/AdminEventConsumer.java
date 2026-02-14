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
        if (statsService.isProcessed(event.getEventId())) return;
        log.info("Consuming booking.created: bookingId={}, gameId={}", event.getBookingId(), event.getGameId());
        statsService.onBookingCreated(event.getGameId(), event.getTotalPrice());
        statsService.markProcessed(event.getEventId(), Topics.BOOKING_CREATED);
    }

    @KafkaListener(topics = Topics.BOOKING_CONFIRMED, groupId = "admin-service")
    public void onBookingConfirmed(BookingEvent event) {
        if (statsService.isProcessed(event.getEventId())) return;
        log.info("Consuming booking.confirmed: bookingId={}, gameId={}", event.getBookingId(), event.getGameId());
        statsService.onBookingConfirmed(event.getGameId());
        statsService.markProcessed(event.getEventId(), Topics.BOOKING_CONFIRMED);
    }

    @KafkaListener(topics = Topics.BOOKING_CANCELLED, groupId = "admin-service")
    public void onBookingCancelled(BookingEvent event) {
        if (statsService.isProcessed(event.getEventId())) return;
        log.info("Consuming booking.cancelled: bookingId={}, gameId={}", event.getBookingId(), event.getGameId());
        statsService.onBookingCancelled(event.getGameId());
        statsService.markProcessed(event.getEventId(), Topics.BOOKING_CANCELLED);
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED, groupId = "admin-service")
    public void onPaymentCompleted(PaymentEvent event) {
        if (statsService.isProcessed(event.getEventId())) return;
        log.info("Consuming payment.completed: paymentId={}, bookingId={}", event.getPaymentId(), event.getBookingId());
        // PaymentEvent doesn't have gameId -- lookup via bookingId is possible,
        // but for now we skip revenue tracking here (already tracked on booking.confirmed)
        statsService.markProcessed(event.getEventId(), Topics.PAYMENT_COMPLETED);
    }

    @KafkaListener(topics = Topics.PAYMENT_REFUNDED, groupId = "admin-service")
    public void onPaymentRefunded(PaymentEvent event) {
        if (statsService.isProcessed(event.getEventId())) return;
        log.info("Consuming payment.refunded: paymentId={}, bookingId={}", event.getPaymentId(), event.getBookingId());
        statsService.markProcessed(event.getEventId(), Topics.PAYMENT_REFUNDED);
    }

    @KafkaListener(topics = Topics.GAME_INFO_UPDATED, groupId = "admin-service")
    public void onGameInfoUpdated(GameInfoUpdatedEvent event) {
        if (statsService.isProcessed(event.getEventId())) return;
        log.info("Consuming game.info-updated: gameId={}", event.getGameId());
        statsService.onGameInfoUpdated(event.getGameId(), event.getHomeTeam(), event.getAwayTeam());
        statsService.markProcessed(event.getEventId(), Topics.GAME_INFO_UPDATED);
    }
}
