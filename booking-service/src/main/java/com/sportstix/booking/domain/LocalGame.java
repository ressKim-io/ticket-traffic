package com.sportstix.booking.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

/**
 * Local replica of game info, synced via Kafka from game-service.
 * Read-only in this service - only updated by Kafka consumer.
 */
@Entity
@Table(name = "local_games")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocalGame implements Persistable<Long> {

    @Id
    private Long id;

    @Column(nullable = false, length = 50)
    private String homeTeam;

    @Column(nullable = false, length = 50)
    private String awayTeam;

    @Column(nullable = false)
    private LocalDateTime gameDate;

    @Column(nullable = false)
    private LocalDateTime ticketOpenAt;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Integer maxTicketsPerUser;

    @Column(nullable = false)
    private LocalDateTime syncedAt;

    @Transient
    private boolean isNew = true;

    public LocalGame(Long id, String homeTeam, String awayTeam,
                     LocalDateTime gameDate, LocalDateTime ticketOpenAt,
                     String status, Integer maxTicketsPerUser) {
        this.id = id;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.gameDate = gameDate;
        this.ticketOpenAt = ticketOpenAt;
        this.status = status;
        this.maxTicketsPerUser = maxTicketsPerUser;
        this.syncedAt = LocalDateTime.now();
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PrePersist
    void markNotNew() {
        this.isNew = false;
    }

    public void updateFrom(String homeTeam, String awayTeam,
                           LocalDateTime gameDate, LocalDateTime ticketOpenAt,
                           String status, Integer maxTicketsPerUser) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.gameDate = gameDate;
        this.ticketOpenAt = ticketOpenAt;
        this.status = status;
        this.maxTicketsPerUser = maxTicketsPerUser;
        this.syncedAt = LocalDateTime.now();
    }
}
