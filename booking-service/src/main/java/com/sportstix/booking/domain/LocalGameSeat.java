package com.sportstix.booking.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Local replica of game seat info, synced via Kafka from game-service.
 * Status is managed locally by booking-service (AVAILABLE -> HELD -> RESERVED).
 */
@Entity
@Table(name = "local_game_seats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocalGameSeat implements Persistable<Long> {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false)
    private Long seatId;

    @Column(nullable = false)
    private Long sectionId;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 10)
    private String rowName;

    private Integer seatNumber;

    @Column(nullable = false)
    private LocalDateTime syncedAt;

    @Transient
    private boolean isNew = true;

    public LocalGameSeat(Long id, Long gameId, Long seatId, Long sectionId,
                         BigDecimal price, String rowName, Integer seatNumber) {
        this.id = id;
        this.gameId = gameId;
        this.seatId = seatId;
        this.sectionId = sectionId;
        this.price = price;
        this.status = "AVAILABLE";
        this.rowName = rowName;
        this.seatNumber = seatNumber;
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

    public void hold() {
        if (!"AVAILABLE".equals(this.status)) {
            throw new IllegalStateException(
                    "Cannot hold seat: current status=" + this.status + ", expected=AVAILABLE");
        }
        this.status = "HELD";
        this.syncedAt = LocalDateTime.now();
    }

    public void reserve() {
        if (!"HELD".equals(this.status)) {
            throw new IllegalStateException(
                    "Cannot reserve seat: current status=" + this.status + ", expected=HELD");
        }
        this.status = "RESERVED";
        this.syncedAt = LocalDateTime.now();
    }

    public void release() {
        if ("AVAILABLE".equals(this.status)) {
            return; // already available, idempotent
        }
        this.status = "AVAILABLE";
        this.syncedAt = LocalDateTime.now();
    }
}
