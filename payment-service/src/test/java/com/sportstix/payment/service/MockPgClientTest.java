package com.sportstix.payment.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MockPgClientTest {

    private final MockPgClient mockPgClient = new MockPgClient();

    @Test
    void charge_returnsSuccessWithTransactionId() {
        MockPgClient.PgResult result = mockPgClient.charge(1L, BigDecimal.valueOf(50000));

        assertThat(result.success()).isTrue();
        assertThat(result.transactionId()).startsWith("PG-");
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void refund_returnsSuccessWithRefundId() {
        MockPgClient.PgResult result = mockPgClient.refund("PG-ABCD1234", BigDecimal.valueOf(50000));

        assertThat(result.success()).isTrue();
        assertThat(result.transactionId()).startsWith("RF-");
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void pgResult_success_hasCorrectFields() {
        MockPgClient.PgResult result = MockPgClient.PgResult.success("TXN-123");

        assertThat(result.success()).isTrue();
        assertThat(result.transactionId()).isEqualTo("TXN-123");
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void pgResult_failure_hasCorrectFields() {
        MockPgClient.PgResult result = MockPgClient.PgResult.failure("Card declined");

        assertThat(result.success()).isFalse();
        assertThat(result.transactionId()).isNull();
        assertThat(result.errorMessage()).isEqualTo("Card declined");
    }
}
