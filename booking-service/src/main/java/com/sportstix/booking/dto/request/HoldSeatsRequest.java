package com.sportstix.booking.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record HoldSeatsRequest(
        @NotNull Long gameId,
        @NotEmpty @Size(max = 4) Set<Long> gameSeatIds
) {
}
