package com.sportstix.payment.event.producer;

import com.sportstix.common.event.Topics;
import com.sportstix.payment.domain.Payment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventProducerTest {

    @Mock
    private ResilientKafkaPublisher publisher;

    @InjectMocks
    private PaymentEventProducer paymentEventProducer;

    @Test
    void publishCompleted_sendsToCorrectTopic() {
        Payment payment = createPayment(1L, 10L, 100L, BigDecimal.valueOf(50000));
        payment.complete("pg-tx-123");

        paymentEventProducer.publishCompleted(payment);

        verify(publisher).publish(
                eq(Topics.PAYMENT_COMPLETED),
                eq("10"),
                any(),
                eq("payment-completed"));
    }

    @Test
    void publishFailed_sendsToCorrectTopic() {
        Payment payment = createPayment(2L, 20L, 200L, BigDecimal.valueOf(30000));
        payment.fail("Insufficient funds");

        paymentEventProducer.publishFailed(payment);

        verify(publisher).publish(
                eq(Topics.PAYMENT_FAILED),
                eq("20"),
                any(),
                eq("payment-failed"));
    }

    @Test
    void publishRefunded_sendsToCorrectTopic() {
        Payment payment = createPayment(3L, 30L, 300L, BigDecimal.valueOf(75000));
        payment.complete("pg-tx-456");
        payment.refund();

        paymentEventProducer.publishRefunded(payment);

        verify(publisher).publish(
                eq(Topics.PAYMENT_REFUNDED),
                eq("30"),
                any(),
                eq("payment-refunded"));
    }

    private Payment createPayment(Long id, Long bookingId, Long userId, BigDecimal amount) {
        Payment payment = Payment.builder()
                .bookingId(bookingId)
                .userId(userId)
                .amount(amount)
                .build();
        try {
            var field = Payment.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(payment, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return payment;
    }
}
