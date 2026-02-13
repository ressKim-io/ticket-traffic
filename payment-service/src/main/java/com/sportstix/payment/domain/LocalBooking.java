package com.sportstix.payment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Local replica of booking info, synced via Kafka from booking-service.
 * Read-only in payment-service - only updated by Kafka consumer.
 */
@Entity
@Table(name = "local_bookings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocalBooking implements Persistable<Long> {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false, length = 20)
    private String status;

    private BigDecimal totalPrice;

    @Column(nullable = false)
    private LocalDateTime syncedAt;

    @Transient
    private boolean isNew = true;

    public LocalBooking(Long id, Long userId, Long gameId, String status, BigDecimal totalPrice) {
        this.id = id;
        this.userId = userId;
        this.gameId = gameId;
        this.status = status;
        this.totalPrice = totalPrice;
        this.syncedAt = LocalDateTime.now();
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PrePersist
    void markNotNew() {
        this.isNew = false;
    }

    public void updateStatus(String status) {
        this.status = status;
        this.syncedAt = LocalDateTime.now();
    }
}
