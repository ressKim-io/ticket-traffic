package com.sportstix.payment.event.producer;

import com.sportstix.common.event.PaymentEvent;
import com.sportstix.common.event.Topics;
import com.sportstix.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final ResilientKafkaPublisher publisher;

    public void publishCompleted(Payment payment) {
        String key = String.valueOf(payment.getBookingId());
        publisher.publish(Topics.PAYMENT_COMPLETED,
                key,
                PaymentEvent.completed(payment.getId(), payment.getBookingId(),
                        payment.getUserId(), payment.getAmount()),
                "payment-completed");
    }

    public void publishFailed(Payment payment) {
        String key = String.valueOf(payment.getBookingId());
        publisher.publish(Topics.PAYMENT_FAILED,
                key,
                PaymentEvent.failed(payment.getId(), payment.getBookingId(),
                        payment.getUserId(), payment.getAmount()),
                "payment-failed");
    }

    public void publishRefunded(Payment payment) {
        String key = String.valueOf(payment.getBookingId());
        publisher.publish(Topics.PAYMENT_REFUNDED,
                key,
                PaymentEvent.refunded(payment.getId(), payment.getBookingId(),
                        payment.getUserId(), payment.getAmount()),
                "payment-refunded");
    }
}
