package com.sportstix.admin.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BookingStatsTest {

    private BookingStats stats;

    @BeforeEach
    void setUp() {
        stats = new BookingStats(1L);
    }

    @Test
    @DisplayName("new BookingStats initializes with zero values")
    void constructor_initializesZeros() {
        assertThat(stats.getGameId()).isEqualTo(1L);
        assertThat(stats.getTotalBookings()).isZero();
        assertThat(stats.getConfirmedBookings()).isZero();
        assertThat(stats.getCancelledBookings()).isZero();
        assertThat(stats.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.getTotalRefunds()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("incrementTotalBookings increases count by 1")
    void incrementTotalBookings() {
        stats.incrementTotalBookings();
        stats.incrementTotalBookings();
        assertThat(stats.getTotalBookings()).isEqualTo(2);
    }

    @Test
    @DisplayName("incrementConfirmed increases confirmed count by 1")
    void incrementConfirmed() {
        stats.incrementConfirmed();
        assertThat(stats.getConfirmedBookings()).isEqualTo(1);
    }

    @Test
    @DisplayName("incrementCancelled increases cancelled count by 1")
    void incrementCancelled() {
        stats.incrementCancelled();
        assertThat(stats.getCancelledBookings()).isEqualTo(1);
    }

    @Test
    @DisplayName("addRevenue accumulates revenue")
    void addRevenue_accumulates() {
        stats.addRevenue(new BigDecimal("30000"));
        stats.addRevenue(new BigDecimal("20000"));
        assertThat(stats.getTotalRevenue()).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("addRevenue with null does nothing")
    void addRevenue_null_noOp() {
        stats.addRevenue(null);
        assertThat(stats.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("addRefund accumulates refunds")
    void addRefund_accumulates() {
        stats.addRefund(new BigDecimal("10000"));
        assertThat(stats.getTotalRefunds()).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("addRefund with null does nothing")
    void addRefund_null_noOp() {
        stats.addRefund(null);
        assertThat(stats.getTotalRefunds()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("updateGameInfo sets team names")
    void updateGameInfo_setsTeams() {
        stats.updateGameInfo("KIA Tigers", "Samsung Lions");
        assertThat(stats.getHomeTeam()).isEqualTo("KIA Tigers");
        assertThat(stats.getAwayTeam()).isEqualTo("Samsung Lions");
    }
}
