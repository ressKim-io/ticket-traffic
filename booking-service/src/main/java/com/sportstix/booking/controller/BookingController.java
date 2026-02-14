package com.sportstix.booking.controller;

import com.sportstix.booking.dto.request.HoldSeatsRequest;
import com.sportstix.booking.dto.response.BookingResponse;
import com.sportstix.booking.service.BookingService;
import com.sportstix.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Booking", description = "Seat reservation and booking management")
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "Hold seats", description = "Reserve seats with 3-tier lock (Redis + DB pessimistic + optimistic)")
    @PostMapping("/hold")
    public ResponseEntity<ApiResponse<BookingResponse>> holdSeats(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody HoldSeatsRequest request) {
        var booking = bookingService.holdSeats(userId, request.gameId(), request.gameSeatIds());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(BookingResponse.from(booking)));
    }

    @Operation(summary = "Confirm booking", description = "Confirm a pending booking (triggers SAGA payment flow)")
    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") Long userId) {
        var booking = bookingService.confirmBooking(bookingId, userId);
        return ResponseEntity.ok(ApiResponse.ok(BookingResponse.from(booking)));
    }

    @Operation(summary = "Cancel booking", description = "Cancel a booking and release held seats")
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") Long userId) {
        var booking = bookingService.cancelBooking(bookingId, userId);
        return ResponseEntity.ok(ApiResponse.ok(BookingResponse.from(booking)));
    }

    @Operation(summary = "Get booking", description = "Get booking details by ID")
    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") Long userId) {
        var booking = bookingService.getBooking(bookingId, userId);
        return ResponseEntity.ok(ApiResponse.ok(BookingResponse.from(booking)));
    }

    @Operation(summary = "List user bookings", description = "Get all bookings for the authenticated user")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getUserBookings(
            @RequestHeader("X-User-Id") Long userId) {
        var bookings = bookingService.getUserBookings(userId).stream()
                .map(BookingResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(bookings));
    }
}
