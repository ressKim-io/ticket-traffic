package com.sportstix.game.dto.response;

import com.sportstix.game.domain.Game;

import java.time.LocalDateTime;
import java.util.List;

public record GameDetailResponse(
        Long id,
        StadiumSummary stadium,
        String homeTeam,
        String awayTeam,
        LocalDateTime gameDate,
        LocalDateTime ticketOpenAt,
        String status,
        int maxTicketsPerUser,
        List<SectionSeatSummary> sections
) {
    public record StadiumSummary(Long id, String name, String address) {}

    public record SectionSeatSummary(
            Long sectionId,
            String sectionName,
            String grade,
            long totalSeats,
            long availableSeats
    ) {}

    public static GameDetailResponse of(Game game, List<SectionSeatSummary> sections) {
        return new GameDetailResponse(
                game.getId(),
                new StadiumSummary(
                        game.getStadium().getId(),
                        game.getStadium().getName(),
                        game.getStadium().getAddress()
                ),
                game.getHomeTeam(),
                game.getAwayTeam(),
                game.getGameDate(),
                game.getTicketOpenAt(),
                game.getStatus().name(),
                game.getMaxTicketsPerUser(),
                sections
        );
    }
}
