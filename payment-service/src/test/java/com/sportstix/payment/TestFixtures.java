package com.sportstix.payment;

import com.sportstix.payment.domain.Payment;

import java.lang.reflect.Field;
import java.math.BigDecimal;

/**
 * Shared test utility for creating entities with preset IDs.
 */
public final class TestFixtures {

    private TestFixtures() {}

    public static void setEntityId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set ID on " + entity.getClass().getSimpleName(), e);
        }
    }

    public static Payment createPaymentWithId(Long id, Long bookingId, Long userId, BigDecimal amount) {
        Payment payment = Payment.builder()
                .bookingId(bookingId)
                .userId(userId)
                .amount(amount)
                .build();
        setEntityId(payment, id);
        return payment;
    }
}
