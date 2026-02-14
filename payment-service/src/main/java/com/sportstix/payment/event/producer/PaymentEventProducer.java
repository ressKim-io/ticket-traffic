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
        publish(Topics.PAYMENT_COMPLETED,
                PaymentEvent.completed(payment.getId(), payment.getBookingId(),
                        payment.getUserId(), payment.getAmount()),
                payment);
    }

    public void publishFailed(Payment payment) {
        publish(Topics.PAYMENT_FAILED,
                PaymentEvent.failed(payment.getId(), payment.getBookingId(),
                        payment.getUserId(), payment.getAmount()),
                payment);
    }

    public void publishRefunded(Payment payment) {
        publish(Topics.PAYMENT_REFUNDED,
                PaymentEvent.refunded(payment.getId(), payment.getBookingId(),
                        payment.getUserId(), payment.getAmount()),
                payment);
    }

    private void publish(String topic, PaymentEvent event, Payment payment) {
        String key = String.valueOf(payment.getBookingId());
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {}: paymentId={}",
                                topic, payment.getId(), ex);
                    } else {
                        log.info("Published {}: paymentId={}, bookingId={}",
                                topic, payment.getId(), payment.getBookingId());
                    }
                });
    }
}
