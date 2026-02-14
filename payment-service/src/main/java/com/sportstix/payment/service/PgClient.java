package com.sportstix.payment.service;

import java.math.BigDecimal;

/**
 * Payment Gateway client interface.
 * Implementations: MockPgClient (dev), real PG adapters (production).
 */
public interface PgClient {

    PgResult charge(Long bookingId, BigDecimal amount);

    PgResult refund(String pgTransactionId, BigDecimal amount);

    record PgResult(boolean success, String transactionId, String errorMessage) {

        public static PgResult success(String transactionId) {
            return new PgResult(true, transactionId, null);
        }

        public static PgResult failure(String errorMessage) {
            return new PgResult(false, null, errorMessage);
        }
    }
}
