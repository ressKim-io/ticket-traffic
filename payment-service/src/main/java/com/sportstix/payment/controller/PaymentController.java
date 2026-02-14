package com.sportstix.payment.controller;

import com.sportstix.common.response.ApiResponse;
import com.sportstix.payment.dto.response.PaymentResponse;
import com.sportstix.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment processed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Payment already exists for this booking")
    })
    @PostMapping("/bookings/{bookingId}/pay")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @PathVariable Long bookingId,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        PaymentResponse response = PaymentResponse.from(
                paymentService.processPayment(bookingId, userId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    @Operation(summary = "Refund payment", description = "Refund a completed payment")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment refunded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Payment not in refundable status")
    })
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable Long paymentId,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        PaymentResponse response = PaymentResponse.from(
                paymentService.refundPayment(paymentId, userId));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Get payment", description = "Get payment details by payment ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable Long paymentId) {
        PaymentResponse response = PaymentResponse.from(paymentService.getPayment(paymentId));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Get payment by booking", description = "Get payment details by booking ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found for booking")
    })
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByBookingId(@PathVariable Long bookingId) {
        PaymentResponse response = PaymentResponse.from(paymentService.getPaymentByBookingId(bookingId));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
