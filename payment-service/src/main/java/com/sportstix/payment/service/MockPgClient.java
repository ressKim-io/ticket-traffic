package com.sportstix.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simulated Payment Gateway client for development/testing.
 * Always succeeds with a generated transaction ID.
 */
@Slf4j
@Component
public class MockPgClient {

    public PgResult charge(Long bookingId, BigDecimal amount) {
        log.info("Mock PG charge: bookingId={}, amount={}", bookingId, amount);

        // Simulate PG processing
        String transactionId = "PG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Mock PG charge successful: txnId={}", transactionId);
        return PgResult.success(transactionId);
    }

    public PgResult refund(String pgTransactionId, BigDecimal amount) {
        log.info("Mock PG refund: txnId={}, amount={}", pgTransactionId, amount);

        String refundId = "RF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Mock PG refund successful: refundId={}", refundId);
        return PgResult.success(refundId);
    }

    public record PgResult(boolean success, String transactionId, String errorMessage) {

        public static PgResult success(String transactionId) {
            return new PgResult(true, transactionId, null);
        }

        public static PgResult failure(String errorMessage) {
            return new PgResult(false, null, errorMessage);
        }
    }
}
