package com.sportstix.admin.repository;

import com.sportstix.admin.domain.BookingStats;
import com.sportstix.admin.dto.response.DashboardResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BookingStatsRepository extends JpaRepository<BookingStats, Long> {

    Optional<BookingStats> findByGameId(Long gameId);

    List<BookingStats> findAllByOrderByUpdatedAtDesc();

    @Query("""
            SELECT new com.sportstix.admin.dto.response.DashboardResponse(
                CAST(COALESCE(SUM(b.totalBookings), 0) AS int),
                CAST(COALESCE(SUM(b.confirmedBookings), 0) AS int),
                COALESCE(SUM(b.totalRevenue), 0),
                COUNT(b)
            )
            FROM BookingStats b
            """)
    DashboardResponse getDashboardAggregates();
}
