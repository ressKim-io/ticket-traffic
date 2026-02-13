package com.sportstix.game.controller;

import com.sportstix.common.response.ApiResponse;
import com.sportstix.game.dto.request.CreateStadiumRequest;
import com.sportstix.game.dto.response.StadiumResponse;
import com.sportstix.game.service.StadiumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stadiums")
@RequiredArgsConstructor
public class StadiumController {

    private final StadiumService stadiumService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StadiumResponse> createStadium(@Valid @RequestBody CreateStadiumRequest request) {
        return ApiResponse.ok(stadiumService.createStadium(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<StadiumResponse> getStadium(@PathVariable Long id) {
        return ApiResponse.ok(stadiumService.getStadium(id));
    }

    @GetMapping
    public ApiResponse<List<StadiumResponse>> getAllStadiums() {
        return ApiResponse.ok(stadiumService.getAllStadiums());
    }
}
