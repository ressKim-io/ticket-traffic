package com.sportstix.admin.controller;

import com.sportstix.admin.dto.response.DashboardResponse;
import com.sportstix.admin.dto.response.GameStatsResponse;
import com.sportstix.admin.service.AdminStatsService;
import com.sportstix.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin", description = "Admin dashboard and statistics")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminStatsService statsService;

    @Operation(summary = "Dashboard overview", description = "Get aggregated booking, revenue, and system stats")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(statsService.getDashboard()));
    }

    @Operation(summary = "Game statistics", description = "Get per-game booking and revenue statistics")
    @GetMapping("/games/stats")
    public ResponseEntity<ApiResponse<List<GameStatsResponse>>> getGameStats() {
        return ResponseEntity.ok(ApiResponse.ok(statsService.getGameStats()));
    }
}
