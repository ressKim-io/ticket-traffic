package com.sportstix.admin.dto.response;

import com.sportstix.admin.domain.BookingStats;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GameStatsResponse(
        Long gameId,
        String homeTeam,
        String awayTeam,
        int totalBookings,
        int confirmedBookings,
        int cancelledBookings,
        BigDecimal totalRevenue,
        BigDecimal totalRefunds,
        LocalDateTime updatedAt
) {
    public static GameStatsResponse from(BookingStats stats) {
        return new GameStatsResponse(
                stats.getGameId(),
                stats.getHomeTeam(),
                stats.getAwayTeam(),
                stats.getTotalBookings(),
                stats.getConfirmedBookings(),
                stats.getCancelledBookings(),
                stats.getTotalRevenue(),
                stats.getTotalRefunds(),
                stats.getUpdatedAt()
        );
    }
}
