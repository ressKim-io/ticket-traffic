package com.sportstix.common.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentEvent extends DomainEvent {

    public static final String TYPE_COMPLETED = "PAYMENT_COMPLETED";
    public static final String TYPE_FAILED = "PAYMENT_FAILED";
    public static final String TYPE_REFUNDED = "PAYMENT_REFUNDED";

    private Long paymentId;
    private Long bookingId;
    private Long userId;
    private BigDecimal amount;

    private PaymentEvent(String eventType, Long paymentId, Long bookingId, Long userId, BigDecimal amount) {
        super(eventType);
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.userId = userId;
        this.amount = amount;
    }

    public static PaymentEvent completed(Long paymentId, Long bookingId, Long userId, BigDecimal amount) {
        return new PaymentEvent(TYPE_COMPLETED, paymentId, bookingId, userId, amount);
    }

    public static PaymentEvent failed(Long paymentId, Long bookingId, Long userId, BigDecimal amount) {
        return new PaymentEvent(TYPE_FAILED, paymentId, bookingId, userId, amount);
    }

    public static PaymentEvent refunded(Long paymentId, Long bookingId, Long userId, BigDecimal amount) {
        return new PaymentEvent(TYPE_REFUNDED, paymentId, bookingId, userId, amount);
    }
}
