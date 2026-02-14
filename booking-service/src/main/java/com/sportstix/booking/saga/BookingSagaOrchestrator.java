package com.sportstix.booking.saga;

import com.sportstix.booking.event.IdempotencyService;
import com.sportstix.common.event.PaymentEvent;
import com.sportstix.common.event.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * SAGA Orchestrator for the Booking-Payment flow.
 *
 * Listens to payment events and drives the SAGA to completion or compensation:
 * - payment.completed → confirm booking (seats HELD→RESERVED)
 * - payment.failed    → compensate (cancel booking, seats→AVAILABLE)
 * - payment.refunded  → compensate (cancel booking, seats→AVAILABLE)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingSagaOrchestrator {

    private final BookingSagaStep bookingSagaStep;
    private final CompensationHandler compensationHandler;
    private final IdempotencyService idempotencyService;

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED, groupId = "booking-saga")
    public void onPaymentCompleted(PaymentEvent event) {
        if (idempotencyService.isDuplicate(event.getEventId(), Topics.PAYMENT_COMPLETED)) {
            log.debug("Duplicate payment-completed event skipped: eventId={}", event.getEventId());
            return;
        }

        log.info("SAGA: payment completed - bookingId={}, paymentId={}",
                event.getBookingId(), event.getPaymentId());

        try {
            bookingSagaStep.confirm(event.getBookingId());
            idempotencyService.markProcessed(event.getEventId(), Topics.PAYMENT_COMPLETED);
            log.info("SAGA completed successfully: bookingId={}", event.getBookingId());
        } catch (Exception e) {
            log.error("SAGA confirm failed, triggering compensation: bookingId={}",
                    event.getBookingId(), e);
            compensationHandler.compensate(event.getBookingId(),
                    "Confirmation failed after payment: " + e.getMessage());
        }
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED, groupId = "booking-saga")
    public void onPaymentFailed(PaymentEvent event) {
        if (idempotencyService.isDuplicate(event.getEventId(), Topics.PAYMENT_FAILED)) {
            log.debug("Duplicate payment-failed event skipped: eventId={}", event.getEventId());
            return;
        }

        log.info("SAGA: payment failed - bookingId={}, paymentId={}",
                event.getBookingId(), event.getPaymentId());

        compensationHandler.compensate(event.getBookingId(), "Payment failed");
        idempotencyService.markProcessed(event.getEventId(), Topics.PAYMENT_FAILED);
    }

    @KafkaListener(topics = Topics.PAYMENT_REFUNDED, groupId = "booking-saga")
    public void onPaymentRefunded(PaymentEvent event) {
        if (idempotencyService.isDuplicate(event.getEventId(), Topics.PAYMENT_REFUNDED)) {
            log.debug("Duplicate payment-refunded event skipped: eventId={}", event.getEventId());
            return;
        }

        log.info("SAGA: payment refunded - bookingId={}, paymentId={}",
                event.getBookingId(), event.getPaymentId());

        compensationHandler.compensate(event.getBookingId(), "Payment refunded");
        idempotencyService.markProcessed(event.getEventId(), Topics.PAYMENT_REFUNDED);
    }
}
