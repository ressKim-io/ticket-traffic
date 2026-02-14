package com.sportstix.payment.controller;

import com.sportstix.common.response.ApiResponse;
import com.sportstix.payment.dto.response.PaymentResponse;
import com.sportstix.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment", description = "Payment processing and refunds")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Process payment", description = "Process payment for a confirmed booking via Mock PG")
    @PostMapping("/bookings/{bookingId}/pay")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") Long userId) {
        PaymentResponse response = PaymentResponse.from(
                paymentService.processPayment(bookingId, userId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    @Operation(summary = "Refund payment", description = "Refund a completed payment")
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable Long paymentId,
            @RequestHeader("X-User-Id") Long userId) {
        PaymentResponse response = PaymentResponse.from(
                paymentService.refundPayment(paymentId, userId));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Get payment", description = "Get payment details by payment ID")
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable Long paymentId) {
        PaymentResponse response = PaymentResponse.from(paymentService.getPayment(paymentId));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Get payment by booking", description = "Get payment details by booking ID")
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByBookingId(@PathVariable Long bookingId) {
        PaymentResponse response = PaymentResponse.from(paymentService.getPaymentByBookingId(bookingId));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
