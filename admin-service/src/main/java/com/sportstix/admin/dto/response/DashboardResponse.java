package com.sportstix.admin.dto.response;

import java.math.BigDecimal;

public record DashboardResponse(
        int totalBookings,
        int confirmedBookings,
        BigDecimal totalRevenue,
        long totalGames
) {
}
