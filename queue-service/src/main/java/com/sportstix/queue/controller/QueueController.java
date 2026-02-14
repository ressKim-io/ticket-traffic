package com.sportstix.queue.controller;

import com.sportstix.common.response.ApiResponse;
import com.sportstix.queue.dto.request.QueueEnterRequest;
import com.sportstix.queue.dto.response.QueueStatusResponse;
import com.sportstix.queue.service.QueueService;
import com.sportstix.queue.service.WaitingRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Queue", description = "Virtual waiting room and queue management")
@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;
    private final WaitingRoomService waitingRoomService;

    @Operation(summary = "Enter queue", description = "Join the ticket purchase queue for a game")
    @PostMapping("/enter")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<QueueStatusResponse> enterQueue(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody QueueEnterRequest request
    ) {
        return ApiResponse.ok(queueService.enterQueue(request.gameId(), userId));
    }

    @Operation(summary = "Queue status", description = "Check current queue position and estimated wait time")
    @GetMapping("/status")
    public ApiResponse<QueueStatusResponse> getQueueStatus(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long gameId
    ) {
        return ApiResponse.ok(queueService.getQueueStatus(gameId, userId));
    }

    @Operation(summary = "Leave queue", description = "Voluntarily leave the queue")
    @DeleteMapping("/leave")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Void> leaveQueue(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long gameId
    ) {
        queueService.leaveQueue(gameId, userId);
        return ApiResponse.ok();
    }

    @Operation(summary = "Register for waiting room", description = "Pre-register for a game before queue opens")
    @PostMapping("/waiting-room/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Void> registerWaitingRoom(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody QueueEnterRequest request
    ) {
        waitingRoomService.register(request.gameId(), userId);
        return ApiResponse.ok();
    }
}
