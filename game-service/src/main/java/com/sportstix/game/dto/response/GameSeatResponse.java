package com.sportstix.game.dto.response;

import com.sportstix.game.domain.GameSeat;

import java.math.BigDecimal;

public record GameSeatResponse(
        Long gameSeatId,
        Long seatId,
        String rowNumber,
        int seatNumber,
        String sectionName,
        String grade,
        BigDecimal price,
        String status
) {
    public static GameSeatResponse from(GameSeat gameSeat) {
        return new GameSeatResponse(
                gameSeat.getId(),
                gameSeat.getSeat().getId(),
                gameSeat.getSeat().getRowNumber(),
                gameSeat.getSeat().getSeatNumber(),
                gameSeat.getSeat().getSection().getName(),
                gameSeat.getSeat().getSection().getGrade().name(),
                gameSeat.getPrice(),
                gameSeat.getStatus().name()
        );
    }
}
