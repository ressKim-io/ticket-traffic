package com.sportstix.payment.event.producer;

import com.sportstix.common.event.PaymentEvent;
import com.sportstix.common.event.Topics;
import com.sportstix.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCompleted(Payment payment) {
        PaymentEvent event = PaymentEvent.completed(
                payment.getId(), payment.getBookingId(),
                payment.getUserId(), payment.getAmount());
        String key = String.valueOf(payment.getBookingId());
        kafkaTemplate.send(Topics.PAYMENT_COMPLETED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish payment-completed: paymentId={}", payment.getId(), ex);
                    } else {
                        log.info("Published payment-completed: paymentId={}, bookingId={}",
                                payment.getId(), payment.getBookingId());
                    }
                });
    }

    public void publishFailed(Payment payment) {
        PaymentEvent event = PaymentEvent.failed(
                payment.getId(), payment.getBookingId(),
                payment.getUserId(), payment.getAmount());
        String key = String.valueOf(payment.getBookingId());
        kafkaTemplate.send(Topics.PAYMENT_FAILED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish payment-failed: paymentId={}", payment.getId(), ex);
                    } else {
                        log.info("Published payment-failed: paymentId={}, bookingId={}",
                                payment.getId(), payment.getBookingId());
                    }
                });
    }

    public void publishRefunded(Payment payment) {
        PaymentEvent event = PaymentEvent.refunded(
                payment.getId(), payment.getBookingId(),
                payment.getUserId(), payment.getAmount());
        String key = String.valueOf(payment.getBookingId());
        kafkaTemplate.send(Topics.PAYMENT_REFUNDED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish payment-refunded: paymentId={}", payment.getId(), ex);
                    } else {
                        log.info("Published payment-refunded: paymentId={}, bookingId={}",
                                payment.getId(), payment.getBookingId());
                    }
                });
    }
}
