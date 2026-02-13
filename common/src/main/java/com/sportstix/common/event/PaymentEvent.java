package com.sportstix.common.event;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class PaymentEvent extends DomainEvent {

    private final Long paymentId;
    private final Long bookingId;
    private final Long userId;
    private final BigDecimal amount;

    private PaymentEvent(String eventType, Long paymentId, Long bookingId, Long userId, BigDecimal amount) {
        super(eventType);
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.userId = userId;
        this.amount = amount;
    }

    public static PaymentEvent completed(Long paymentId, Long bookingId, Long userId, BigDecimal amount) {
        return new PaymentEvent("PAYMENT_COMPLETED", paymentId, bookingId, userId, amount);
    }

    public static PaymentEvent failed(Long paymentId, Long bookingId, Long userId, BigDecimal amount) {
        return new PaymentEvent("PAYMENT_FAILED", paymentId, bookingId, userId, amount);
    }

    public static PaymentEvent refunded(Long paymentId, Long bookingId, Long userId, BigDecimal amount) {
        return new PaymentEvent("PAYMENT_REFUNDED", paymentId, bookingId, userId, amount);
    }
}
