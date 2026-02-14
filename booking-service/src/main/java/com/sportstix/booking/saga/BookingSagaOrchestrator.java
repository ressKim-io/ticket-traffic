package com.sportstix.booking.saga;

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

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED, groupId = "booking-saga")
    public void onPaymentCompleted(PaymentEvent event) {
        log.info("SAGA: payment completed - bookingId={}, paymentId={}",
                event.getBookingId(), event.getPaymentId());

        try {
            bookingSagaStep.confirm(event.getBookingId());
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
        log.info("SAGA: payment failed - bookingId={}, paymentId={}",
                event.getBookingId(), event.getPaymentId());

        compensationHandler.compensate(event.getBookingId(), "Payment failed");
    }

    @KafkaListener(topics = Topics.PAYMENT_REFUNDED, groupId = "booking-saga")
    public void onPaymentRefunded(PaymentEvent event) {
        log.info("SAGA: payment refunded - bookingId={}, paymentId={}",
                event.getBookingId(), event.getPaymentId());

        compensationHandler.compensate(event.getBookingId(), "Payment refunded");
    }
}
