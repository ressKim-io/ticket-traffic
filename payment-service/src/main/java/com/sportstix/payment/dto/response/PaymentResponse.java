package com.sportstix.payment.dto.response;

import com.sportstix.payment.domain.Payment;
import com.sportstix.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long bookingId,
        Long userId,
        BigDecimal amount,
        PaymentStatus status,
        String pgTransactionId,
        String failureReason,
        LocalDateTime createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBookingId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPgTransactionId(),
                payment.getFailureReason(),
                payment.getCreatedAt()
        );
    }
}
