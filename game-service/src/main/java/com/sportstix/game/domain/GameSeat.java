package com.sportstix.game.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_seats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(nullable = false, precision = 10, scale = 0)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameSeatStatus status;

    private Long heldByBookingId;

    private LocalDateTime heldAt;

    @Version
    private Long version;

    @Builder
    private GameSeat(Game game, Seat seat, BigDecimal price) {
        this.game = game;
        this.seat = seat;
        this.price = price;
        this.status = GameSeatStatus.AVAILABLE;
    }

    public void hold(Long bookingId) {
        this.status = GameSeatStatus.HELD;
        this.heldByBookingId = bookingId;
        this.heldAt = LocalDateTime.now();
    }

    public void reserve() {
        this.status = GameSeatStatus.RESERVED;
    }

    public void release() {
        this.status = GameSeatStatus.AVAILABLE;
        this.heldByBookingId = null;
        this.heldAt = null;
    }
}
