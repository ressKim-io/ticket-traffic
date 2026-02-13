package com.sportstix.game.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record CreateGameRequest(
        @NotNull Long stadiumId,
        @NotBlank String homeTeam,
        @NotBlank String awayTeam,
        @NotNull @Future LocalDateTime gameDate,
        @NotNull @Future LocalDateTime ticketOpenAt,
        @Positive int maxTicketsPerUser
) {
}
