package com.sportstix.admin.controller;

import com.sportstix.admin.dto.response.DashboardResponse;
import com.sportstix.admin.dto.response.GameStatsResponse;
import com.sportstix.admin.service.AdminStatsService;
import com.sportstix.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminStatsService statsService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(statsService.getDashboard()));
    }

    @GetMapping("/games/stats")
    public ResponseEntity<ApiResponse<List<GameStatsResponse>>> getGameStats() {
        return ResponseEntity.ok(ApiResponse.ok(statsService.getGameStats()));
    }
}
