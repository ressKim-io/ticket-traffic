package com.sportstix.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(400, "C001", "Invalid input"),
    RESOURCE_NOT_FOUND(404, "C002", "Resource not found"),
    INTERNAL_ERROR(500, "C003", "Internal server error"),
    UNAUTHORIZED(401, "C004", "Unauthorized"),
    FORBIDDEN(403, "C005", "Forbidden"),

    // Auth
    INVALID_TOKEN(401, "A001", "Invalid or expired token"),
    DUPLICATE_EMAIL(409, "A002", "Email already exists"),
    LOGIN_FAILED(401, "A003", "Invalid credentials"),

    // Game
    GAME_NOT_FOUND(404, "G001", "Game not found"),
    GAME_NOT_OPEN(400, "G002", "Game is not open for booking"),

    // Queue
    QUEUE_NOT_OPEN(400, "Q001", "Queue is not open"),
    INVALID_QUEUE_TOKEN(401, "Q002", "Invalid or expired queue token"),
    QUEUE_POSITION_NOT_REACHED(403, "Q003", "Queue position not reached"),

    // Booking
    SEAT_ALREADY_HELD(409, "B001", "Seat is already held"),
    SEAT_NOT_AVAILABLE(400, "B002", "Seat is not available"),
    BOOKING_NOT_FOUND(404, "B003", "Booking not found"),
    BOOKING_EXPIRED(400, "B004", "Booking has expired"),
    LOCK_ACQUISITION_FAILED(409, "B005", "Failed to acquire lock"),

    // Payment
    PAYMENT_FAILED(400, "P001", "Payment processing failed"),
    PAYMENT_ALREADY_COMPLETED(409, "P002", "Payment already completed"),
    REFUND_FAILED(400, "P003", "Refund processing failed");

    private final int status;
    private final String code;
    private final String message;
}
