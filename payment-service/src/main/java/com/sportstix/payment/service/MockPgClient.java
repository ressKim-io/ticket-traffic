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
public class MockPgClient implements PgClient {

    @Override
    public PgResult charge(Long bookingId, BigDecimal amount) {
        log.info("Mock PG charge: bookingId={}, amount={}", bookingId, amount);

        String transactionId = "PG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Mock PG charge successful: txnId={}", transactionId);
        return PgResult.success(transactionId);
    }

    @Override
    public PgResult refund(String pgTransactionId, BigDecimal amount) {
        log.info("Mock PG refund: txnId={}, amount={}", pgTransactionId, amount);

        String refundId = "RF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Mock PG refund successful: refundId={}", refundId);
        return PgResult.success(refundId);
    }
}
