package com.sportstix.game.controller;

import com.sportstix.common.response.ApiResponse;
import com.sportstix.game.domain.GameStatus;
import com.sportstix.game.dto.request.CreateGameRequest;
import com.sportstix.game.dto.response.GameDetailResponse;
import com.sportstix.game.dto.response.GameResponse;
import com.sportstix.game.dto.response.GameSeatResponse;
import com.sportstix.game.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GameResponse> createGame(@Valid @RequestBody CreateGameRequest request) {
        return ApiResponse.ok(gameService.createGame(request));
    }

    @GetMapping("/{gameId}")
    public ApiResponse<GameDetailResponse> getGame(@PathVariable Long gameId) {
        return ApiResponse.ok(gameService.getGame(gameId));
    }

    @GetMapping
    public ApiResponse<Page<GameResponse>> getGames(
            @RequestParam(required = false) GameStatus status,
            @RequestParam(required = false) String teamName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.plusDays(1).atStartOfDay() : null;

        return ApiResponse.ok(gameService.getGames(status, teamName, fromDateTime, toDateTime, pageable));
    }

    @GetMapping("/{gameId}/seats")
    public ApiResponse<List<GameSeatResponse>> getGameSeats(
            @PathVariable Long gameId,
            @RequestParam Long sectionId
    ) {
        return ApiResponse.ok(gameService.getGameSeats(gameId, sectionId));
    }
}
