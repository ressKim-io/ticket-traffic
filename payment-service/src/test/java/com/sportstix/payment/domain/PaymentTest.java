package com.sportstix.payment.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void builder_createsPaymentWithPendingStatus() {
        Payment payment = Payment.builder()
                .bookingId(1L)
                .userId(100L)
                .amount(BigDecimal.valueOf(50000))
                .build();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getBookingId()).isEqualTo(1L);
        assertThat(payment.getUserId()).isEqualTo(100L);
        assertThat(payment.getAmount()).isEqualTo(BigDecimal.valueOf(50000));
    }

    @Test
    void complete_fromPending_setsCompletedWithTxnId() {
        Payment payment = createPendingPayment();

        payment.complete("PG-12345");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getPgTransactionId()).isEqualTo("PG-12345");
    }

    @Test
    void complete_fromNonPending_throwsException() {
        Payment payment = createPendingPayment();
        payment.complete("PG-12345");

        assertThatThrownBy(() -> payment.complete("PG-99999"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot complete payment");
    }

    @Test
    void fail_fromPending_setsFailedWithReason() {
        Payment payment = createPendingPayment();

        payment.fail("Insufficient funds");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("Insufficient funds");
    }

    @Test
    void fail_fromNonPending_throwsException() {
        Payment payment = createPendingPayment();
        payment.fail("Some reason");

        assertThatThrownBy(() -> payment.fail("Another reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot fail payment");
    }

    @Test
    void refund_fromCompleted_setsRefunded() {
        Payment payment = createPendingPayment();
        payment.complete("PG-12345");

        payment.refund();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void refund_fromNonCompleted_throwsException() {
        Payment payment = createPendingPayment();

        assertThatThrownBy(() -> payment.refund())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot refund payment");
    }

    private Payment createPendingPayment() {
        return Payment.builder()
                .bookingId(1L)
                .userId(100L)
                .amount(BigDecimal.valueOf(50000))
                .build();
    }
}
