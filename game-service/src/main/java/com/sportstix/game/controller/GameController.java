package com.sportstix.game.controller;

import com.sportstix.common.response.ApiResponse;
import com.sportstix.game.domain.GameStatus;
import com.sportstix.game.dto.request.CreateGameRequest;
import com.sportstix.game.dto.response.GameDetailResponse;
import com.sportstix.game.dto.response.GameResponse;
import com.sportstix.game.dto.response.GameSeatResponse;
import com.sportstix.game.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Game", description = "Game management and seat browsing")
@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @Operation(summary = "Create game", description = "Create a new game and initialize seats from stadium")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Game created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Stadium not found")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GameResponse> createGame(@Valid @RequestBody CreateGameRequest request) {
        return ApiResponse.ok(gameService.createGame(request));
    }

    @Operation(summary = "Get game detail", description = "Get game details with section availability")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Game found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Game not found")
    })
    @GetMapping("/{gameId}")
    public ApiResponse<GameDetailResponse> getGame(@PathVariable Long gameId) {
        return ApiResponse.ok(gameService.getGame(gameId));
    }

    @Operation(summary = "List games", description = "Browse games with filters (status, team, date range)")
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

    @Operation(summary = "Get seats by section", description = "List seats for a game section with status and price")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seats returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Game or section not found")
    })
    @GetMapping("/{gameId}/seats")
    public ApiResponse<List<GameSeatResponse>> getGameSeats(
            @PathVariable Long gameId,
            @RequestParam Long sectionId
    ) {
        return ApiResponse.ok(gameService.getGameSeats(gameId, sectionId));
    }
}
