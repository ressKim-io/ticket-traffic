package com.sportstix.booking.domain;

import com.sportstix.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {

    @Test
    void builder_createsBookingWithPendingStatus() {
        Booking booking = Booking.builder()
                .userId(100L)
                .gameId(10L)
                .build();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getTotalPrice()).isEqualTo(BigDecimal.ZERO);
        assertThat(booking.getHoldExpiresAt()).isNotNull();
    }

    @Test
    void addSeat_increasesTotalPrice() {
        Booking booking = Booking.builder().userId(100L).gameId(10L).build();

        booking.addSeat(1L, BigDecimal.valueOf(50000));
        booking.addSeat(2L, BigDecimal.valueOf(30000));

        assertThat(booking.getTotalPrice()).isEqualTo(BigDecimal.valueOf(80000));
        assertThat(booking.getBookingSeats()).hasSize(2);
    }

    @Test
    void confirm_fromPending_setsConfirmed() {
        Booking booking = Booking.builder().userId(100L).gameId(10L).build();

        booking.confirm();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getHoldExpiresAt()).isNull();
    }

    @Test
    void confirm_fromNonPending_throwsException() {
        Booking booking = Booking.builder().userId(100L).gameId(10L).build();
        booking.cancel();

        assertThatThrownBy(() -> booking.confirm())
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void cancel_isIdempotent() {
        Booking booking = Booking.builder().userId(100L).gameId(10L).build();
        booking.cancel();

        // Second cancel should not throw
        booking.cancel();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }
}
