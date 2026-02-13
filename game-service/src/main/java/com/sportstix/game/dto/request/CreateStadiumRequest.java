package com.sportstix.game.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateStadiumRequest(
        @NotBlank String name,
        @NotBlank String address,
        @Positive int totalCapacity,
        @NotEmpty @Valid List<CreateSectionRequest> sections
) {
}
