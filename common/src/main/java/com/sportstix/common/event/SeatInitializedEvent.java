package com.sportstix.common.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Published when game seats are initialized.
 * Contains game info and seat details for booking-service local replica sync.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatInitializedEvent extends DomainEvent {

    public static final String TYPE = "SEAT_INITIALIZED";

    private Long gameId;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime gameDate;
    private LocalDateTime ticketOpenAt;
    private String status;
    private Integer maxTicketsPerUser;
    private List<SeatInfo> seats;

    public SeatInitializedEvent(Long gameId, String homeTeam, String awayTeam,
                                 LocalDateTime gameDate, LocalDateTime ticketOpenAt,
                                 String status, Integer maxTicketsPerUser,
                                 List<SeatInfo> seats) {
        super(TYPE);
        this.gameId = gameId;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.gameDate = gameDate;
        this.ticketOpenAt = ticketOpenAt;
        this.status = status;
        this.maxTicketsPerUser = maxTicketsPerUser;
        this.seats = seats;
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class SeatInfo {
        private Long gameSeatId;
        private Long seatId;
        private Long sectionId;
        private BigDecimal price;
        private String rowName;
        private Integer seatNumber;

        public SeatInfo(Long gameSeatId, Long seatId, Long sectionId,
                        BigDecimal price, String rowName, Integer seatNumber) {
            this.gameSeatId = gameSeatId;
            this.seatId = seatId;
            this.sectionId = sectionId;
            this.price = price;
            this.rowName = rowName;
            this.seatNumber = seatNumber;
        }
    }
}
