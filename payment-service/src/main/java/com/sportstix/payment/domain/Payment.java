package com.sportstix.payment.domain;

import com.sportstix.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long bookingId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(length = 100)
    private String pgTransactionId;

    @Column(length = 255)
    private String failureReason;

    @Builder
    private Payment(Long bookingId, Long userId, BigDecimal amount) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    public void complete(String pgTransactionId) {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot complete payment: current status=" + this.status);
        }
        this.status = PaymentStatus.COMPLETED;
        this.pgTransactionId = pgTransactionId;
    }

    public void fail(String reason) {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot fail payment: current status=" + this.status);
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public void refund() {
        if (this.status != PaymentStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Cannot refund payment: current status=" + this.status);
        }
        this.status = PaymentStatus.REFUNDED;
    }
}
