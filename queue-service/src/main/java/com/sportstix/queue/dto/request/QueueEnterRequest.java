package com.sportstix.queue.dto.request;

import jakarta.validation.constraints.NotNull;

public record QueueEnterRequest(
        @NotNull Long gameId
) {
}
