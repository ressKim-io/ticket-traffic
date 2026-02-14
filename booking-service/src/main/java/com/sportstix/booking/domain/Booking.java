package com.sportstix.booking.domain;

import com.sportstix.common.domain.BaseTimeEntity;
import com.sportstix.common.exception.BusinessException;
import com.sportstix.common.response.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking extends BaseTimeEntity {

    private static final int HOLD_MINUTES = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long gameId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    private LocalDateTime holdExpiresAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingSeat> bookingSeats = new ArrayList<>();

    @Builder
    private Booking(Long userId, Long gameId) {
        this.userId = userId;
        this.gameId = gameId;
        this.status = BookingStatus.PENDING;
        this.totalPrice = BigDecimal.ZERO;
        this.holdExpiresAt = LocalDateTime.now().plusMinutes(HOLD_MINUTES);
    }

    public void addSeat(Long gameSeatId, BigDecimal price) {
        BookingSeat bookingSeat = new BookingSeat(this, gameSeatId, price);
        this.bookingSeats.add(bookingSeat);
        this.totalPrice = this.totalPrice.add(price);
    }

    public void confirm() {
        if (this.status != BookingStatus.PENDING) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_FOUND,
                    "Cannot confirm booking: current status=" + this.status);
        }
        this.status = BookingStatus.CONFIRMED;
        this.holdExpiresAt = null;
    }

    public void cancel() {
        if (this.status == BookingStatus.CANCELLED) {
            return; // idempotent
        }
        this.status = BookingStatus.CANCELLED;
        this.holdExpiresAt = null;
    }

    public boolean isExpired() {
        return this.status == BookingStatus.PENDING
                && this.holdExpiresAt != null
                && LocalDateTime.now().isAfter(this.holdExpiresAt);
    }
}
