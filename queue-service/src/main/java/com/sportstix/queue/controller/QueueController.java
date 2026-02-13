package com.sportstix.queue.controller;

import com.sportstix.common.response.ApiResponse;
import com.sportstix.queue.dto.request.QueueEnterRequest;
import com.sportstix.queue.dto.response.QueueStatusResponse;
import com.sportstix.queue.service.QueueService;
import com.sportstix.queue.service.WaitingRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;
    private final WaitingRoomService waitingRoomService;

    @PostMapping("/enter")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<QueueStatusResponse> enterQueue(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody QueueEnterRequest request
    ) {
        return ApiResponse.ok(queueService.enterQueue(request.gameId(), userId));
    }

    @GetMapping("/status")
    public ApiResponse<QueueStatusResponse> getQueueStatus(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long gameId
    ) {
        return ApiResponse.ok(queueService.getQueueStatus(gameId, userId));
    }

    @DeleteMapping("/leave")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Void> leaveQueue(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long gameId
    ) {
        queueService.leaveQueue(gameId, userId);
        return ApiResponse.ok();
    }

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
