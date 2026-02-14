package com.sportstix.booking.controller;

import com.sportstix.booking.dto.request.HoldSeatsRequest;
import com.sportstix.booking.dto.response.BookingResponse;
import com.sportstix.booking.service.BookingService;
import com.sportstix.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/hold")
    public ResponseEntity<ApiResponse<BookingResponse>> holdSeats(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody HoldSeatsRequest request) {
        var booking = bookingService.holdSeats(userId, request.gameId(), request.gameSeatIds());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(BookingResponse.from(booking)));
    }

    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") Long userId) {
        var booking = bookingService.confirmBooking(bookingId, userId);
        return ResponseEntity.ok(ApiResponse.ok(BookingResponse.from(booking)));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") Long userId) {
        var booking = bookingService.cancelBooking(bookingId, userId);
        return ResponseEntity.ok(ApiResponse.ok(BookingResponse.from(booking)));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") Long userId) {
        var booking = bookingService.getBooking(bookingId, userId);
        return ResponseEntity.ok(ApiResponse.ok(BookingResponse.from(booking)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getUserBookings(
            @RequestHeader("X-User-Id") Long userId) {
        var bookings = bookingService.getUserBookings(userId).stream()
                .map(BookingResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(bookings));
    }
}
