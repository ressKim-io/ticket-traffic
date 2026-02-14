package com.sportstix.booking.dto.response;

import com.sportstix.booking.domain.Booking;
import com.sportstix.booking.domain.BookingSeat;
import com.sportstix.booking.domain.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        Long bookingId,
        Long userId,
        Long gameId,
        BookingStatus status,
        BigDecimal totalPrice,
        LocalDateTime holdExpiresAt,
        List<SeatInfo> seats,
        LocalDateTime createdAt
) {
    public record SeatInfo(Long gameSeatId, BigDecimal price) {
    }

    public static BookingResponse from(Booking booking) {
        List<SeatInfo> seats = booking.getBookingSeats().stream()
                .map(bs -> new SeatInfo(bs.getGameSeatId(), bs.getPrice()))
                .toList();
        return new BookingResponse(
                booking.getId(),
                booking.getUserId(),
                booking.getGameId(),
                booking.getStatus(),
                booking.getTotalPrice(),
                booking.getHoldExpiresAt(),
                seats,
                booking.getCreatedAt()
        );
    }
}
