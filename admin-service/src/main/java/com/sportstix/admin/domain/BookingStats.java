package com.sportstix.admin.domain;

import com.sportstix.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "booking_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingStats extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long gameId;

    private String homeTeam;
    private String awayTeam;

    @Column(nullable = false)
    private int totalBookings;

    @Column(nullable = false)
    private int confirmedBookings;

    @Column(nullable = false)
    private int cancelledBookings;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalRevenue;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalRefunds;

    @Version
    private Long version;

    public BookingStats(Long gameId) {
        this.gameId = gameId;
        this.totalBookings = 0;
        this.confirmedBookings = 0;
        this.cancelledBookings = 0;
        this.totalRevenue = BigDecimal.ZERO;
        this.totalRefunds = BigDecimal.ZERO;
    }

    public void updateGameInfo(String homeTeam, String awayTeam) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
    }

    public void incrementTotalBookings() {
        this.totalBookings++;
    }

    public void incrementConfirmed() {
        this.confirmedBookings++;
    }

    public void incrementCancelled() {
        this.cancelledBookings++;
    }

    public void addRevenue(BigDecimal amount) {
        if (amount != null) {
            this.totalRevenue = this.totalRevenue.add(amount);
        }
    }

    public void addRefund(BigDecimal amount) {
        if (amount != null) {
            this.totalRefunds = this.totalRefunds.add(amount);
        }
    }
}
