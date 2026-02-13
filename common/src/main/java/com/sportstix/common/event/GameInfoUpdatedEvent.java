package com.sportstix.common.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Published when game info is updated.
 * Used by booking-service to sync local_games replica.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameInfoUpdatedEvent extends DomainEvent {

    public static final String TYPE = "GAME_INFO_UPDATED";

    private Long gameId;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime gameDate;
    private LocalDateTime ticketOpenAt;
    private String status;
    private Integer maxTicketsPerUser;

    public GameInfoUpdatedEvent(Long gameId, String homeTeam, String awayTeam,
                                 LocalDateTime gameDate, LocalDateTime ticketOpenAt,
                                 String status, Integer maxTicketsPerUser) {
        super(TYPE);
        this.gameId = gameId;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.gameDate = gameDate;
        this.ticketOpenAt = ticketOpenAt;
        this.status = status;
        this.maxTicketsPerUser = maxTicketsPerUser;
    }
}
