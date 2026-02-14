package com.sportstix.booking.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalGameSeatTest {

    @Test
    void constructor_initializesWithAvailableStatus() {
        LocalGameSeat seat = createSeat(1L, 10L);

        assertThat(seat.getStatus()).isEqualTo("AVAILABLE");
        assertThat(seat.getGameId()).isEqualTo(10L);
        assertThat(seat.isNew()).isTrue();
    }

    @Test
    void hold_fromAvailable_setsHeld() {
        LocalGameSeat seat = createSeat(1L, 10L);

        seat.hold();

        assertThat(seat.getStatus()).isEqualTo("HELD");
    }

    @Test
    void hold_fromNonAvailable_throwsException() {
        LocalGameSeat seat = createSeat(1L, 10L);
        seat.hold(); // now HELD

        assertThatThrownBy(() -> seat.hold())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expected=AVAILABLE");
    }

    @Test
    void reserve_fromHeld_setsReserved() {
        LocalGameSeat seat = createSeat(1L, 10L);
        seat.hold();

        seat.reserve();

        assertThat(seat.getStatus()).isEqualTo("RESERVED");
    }

    @Test
    void reserve_fromNonHeld_throwsException() {
        LocalGameSeat seat = createSeat(1L, 10L);

        assertThatThrownBy(() -> seat.reserve())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expected=HELD");
    }

    @Test
    void release_fromHeld_setsAvailable() {
        LocalGameSeat seat = createSeat(1L, 10L);
        seat.hold();

        seat.release();

        assertThat(seat.getStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    void release_fromReserved_setsAvailable() {
        LocalGameSeat seat = createSeat(1L, 10L);
        seat.hold();
        seat.reserve();

        seat.release();

        assertThat(seat.getStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    void release_alreadyAvailable_isIdempotent() {
        LocalGameSeat seat = createSeat(1L, 10L);

        seat.release(); // no-op

        assertThat(seat.getStatus()).isEqualTo("AVAILABLE");
    }

    private LocalGameSeat createSeat(Long id, Long gameId) {
        return new LocalGameSeat(id, gameId, id, 1L,
                BigDecimal.valueOf(50000), "A", 1);
    }
}
