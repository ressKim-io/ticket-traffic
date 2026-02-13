package com.sportstix.game.dto.request;

import com.sportstix.game.domain.SeatGrade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateSectionRequest(
        @NotBlank String name,
        @NotNull SeatGrade grade,
        @Positive int rows,
        @Positive int seatsPerRow
) {
}
