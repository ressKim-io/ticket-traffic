package com.sportstix.payment.controller;

import com.sportstix.common.response.ApiResponse;
import com.sportstix.payment.dto.response.PaymentResponse;
import com.sportstix.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/bookings/{bookingId}/pay")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(@PathVariable Long bookingId) {
        PaymentResponse response = PaymentResponse.from(paymentService.processPayment(bookingId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(@PathVariable Long paymentId) {
        PaymentResponse response = PaymentResponse.from(paymentService.refundPayment(paymentId));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable Long paymentId) {
        PaymentResponse response = PaymentResponse.from(paymentService.getPayment(paymentId));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByBookingId(@PathVariable Long bookingId) {
        PaymentResponse response = PaymentResponse.from(paymentService.getPaymentByBookingId(bookingId));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
