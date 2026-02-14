package com.sportstix.admin.repository;

import com.sportstix.admin.domain.BookingStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BookingStatsRepository extends JpaRepository<BookingStats, Long> {

    Optional<BookingStats> findByGameId(Long gameId);

    List<BookingStats> findAllByOrderByUpdatedAtDesc();

    @Query("SELECT COALESCE(SUM(b.totalBookings), 0) FROM BookingStats b")
    int sumTotalBookings();

    @Query("SELECT COALESCE(SUM(b.confirmedBookings), 0) FROM BookingStats b")
    int sumConfirmedBookings();

    @Query("SELECT COALESCE(SUM(b.totalRevenue), 0) FROM BookingStats b")
    BigDecimal sumTotalRevenue();
}
