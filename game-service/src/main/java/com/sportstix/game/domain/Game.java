package com.sportstix.game.domain;

import com.sportstix.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "games")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadium_id", nullable = false)
    private Stadium stadium;

    @Column(nullable = false, length = 50)
    private String homeTeam;

    @Column(nullable = false, length = 50)
    private String awayTeam;

    @Column(nullable = false)
    private LocalDateTime gameDate;

    @Column(nullable = false)
    private LocalDateTime ticketOpenAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameStatus status;

    @Column(nullable = false)
    private int maxTicketsPerUser;

    @Builder
    private Game(Stadium stadium, String homeTeam, String awayTeam,
                 LocalDateTime gameDate, LocalDateTime ticketOpenAt,
                 GameStatus status, int maxTicketsPerUser) {
        this.stadium = stadium;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.gameDate = gameDate;
        this.ticketOpenAt = ticketOpenAt;
        this.status = status != null ? status : GameStatus.SCHEDULED;
        this.maxTicketsPerUser = maxTicketsPerUser > 0 ? maxTicketsPerUser : 4;
    }

    public void updateStatus(GameStatus status) {
        this.status = status;
    }
}
