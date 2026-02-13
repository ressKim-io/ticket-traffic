package com.sportstix.game.dto.response;

import com.sportstix.game.domain.Game;

import java.time.LocalDateTime;

public record GameResponse(
        Long id,
        String stadiumName,
        String homeTeam,
        String awayTeam,
        LocalDateTime gameDate,
        LocalDateTime ticketOpenAt,
        String status,
        int maxTicketsPerUser
) {
    public static GameResponse from(Game game) {
        return new GameResponse(
                game.getId(),
                game.getStadium().getName(),
                game.getHomeTeam(),
                game.getAwayTeam(),
                game.getGameDate(),
                game.getTicketOpenAt(),
                game.getStatus().name(),
                game.getMaxTicketsPerUser()
        );
    }
}
