-- Payment records owned by payment-service
CREATE TABLE payments (
    id              BIGSERIAL       PRIMARY KEY,
    booking_id      BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    amount          DECIMAL(10,0)   NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    pg_transaction_id VARCHAR(100),
    failure_reason  VARCHAR(255),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_payments_booking ON payments(booking_id);
CREATE INDEX idx_payments_user ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
