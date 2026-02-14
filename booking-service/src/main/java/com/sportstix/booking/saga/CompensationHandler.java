package com.sportstix.booking.saga;

import com.sportstix.booking.service.BookingTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SAGA compensation: releases booking and seats when payment fails.
 * Delegates to BookingTransactionService for proper transaction boundary.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompensationHandler {

    private final BookingTransactionService transactionService;

    public void compensate(Long bookingId, String reason) {
        log.info("SAGA compensation: bookingId={}, reason={}", bookingId, reason);
        try {
            transactionService.releaseBookingById(bookingId);
            log.info("SAGA compensation completed: bookingId={}", bookingId);
        } catch (Exception e) {
            log.error("SAGA compensation failed: bookingId={}", bookingId, e);
        }
    }
}
