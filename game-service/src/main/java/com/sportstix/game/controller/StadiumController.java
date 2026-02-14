package com.sportstix.game.controller;

import com.sportstix.common.response.ApiResponse;
import com.sportstix.game.dto.request.CreateStadiumRequest;
import com.sportstix.game.dto.response.StadiumResponse;
import com.sportstix.game.service.StadiumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Stadium", description = "Stadium and section management")
@RestController
@RequestMapping("/api/v1/stadiums")
@RequiredArgsConstructor
public class StadiumController {

    private final StadiumService stadiumService;

    @Operation(summary = "Create stadium", description = "Create a new stadium with sections and seats")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StadiumResponse> createStadium(@Valid @RequestBody CreateStadiumRequest request) {
        return ApiResponse.ok(stadiumService.createStadium(request));
    }

    @Operation(summary = "Get stadium", description = "Get stadium details with sections")
    @GetMapping("/{id}")
    public ApiResponse<StadiumResponse> getStadium(@PathVariable Long id) {
        return ApiResponse.ok(stadiumService.getStadium(id));
    }

    @Operation(summary = "List stadiums", description = "Get all registered stadiums")
    @GetMapping
    public ApiResponse<List<StadiumResponse>> getAllStadiums() {
        return ApiResponse.ok(stadiumService.getAllStadiums());
    }
}
