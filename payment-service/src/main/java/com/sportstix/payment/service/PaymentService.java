package com.sportstix.payment.service;

import com.sportstix.common.exception.BusinessException;
import com.sportstix.common.response.ErrorCode;
import com.sportstix.payment.domain.LocalBooking;
import com.sportstix.payment.domain.Payment;
import com.sportstix.payment.domain.PaymentStatus;
import com.sportstix.payment.event.producer.PaymentEventProducer;
import com.sportstix.payment.repository.LocalBookingRepository;
import com.sportstix.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final LocalBookingRepository localBookingRepository;
    private final MockPgClient mockPgClient;
    private final PaymentEventProducer paymentEventProducer;

    @Transactional
    public Payment processPayment(Long bookingId) {
        log.info("Processing payment for bookingId={}", bookingId);

        // Idempotency: reject if already completed or pending
        if (paymentRepository.existsByBookingIdAndStatusIn(bookingId,
                List.of(PaymentStatus.PENDING, PaymentStatus.COMPLETED))) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_COMPLETED,
                    "Payment already exists for booking: " + bookingId);
        }

        // Look up local booking replica for user and amount
        LocalBooking booking = localBookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND,
                        "Booking not found: " + bookingId));

        if (booking.getTotalPrice() == null) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED,
                    "Booking total price not available: " + bookingId);
        }

        // Create payment in PENDING status
        Payment payment = Payment.builder()
                .bookingId(bookingId)
                .userId(booking.getUserId())
                .amount(booking.getTotalPrice())
                .build();
        payment = paymentRepository.save(payment);

        // Call Mock PG
        MockPgClient.PgResult pgResult = mockPgClient.charge(bookingId, payment.getAmount());

        if (pgResult.success()) {
            payment.complete(pgResult.transactionId());
            paymentRepository.save(payment);
            paymentEventProducer.publishCompleted(payment);
            log.info("Payment completed: paymentId={}, pgTxnId={}",
                    payment.getId(), pgResult.transactionId());
        } else {
            payment.fail(pgResult.errorMessage());
            paymentRepository.save(payment);
            paymentEventProducer.publishFailed(payment);
            log.warn("Payment failed: paymentId={}, reason={}",
                    payment.getId(), pgResult.errorMessage());
        }

        return payment;
    }

    @Transactional
    public Payment refundPayment(Long paymentId) {
        log.info("Processing refund for paymentId={}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Payment not found: " + paymentId));

        MockPgClient.PgResult pgResult = mockPgClient.refund(
                payment.getPgTransactionId(), payment.getAmount());

        if (pgResult.success()) {
            payment.refund();
            paymentRepository.save(payment);
            paymentEventProducer.publishRefunded(payment);
            log.info("Refund completed: paymentId={}", paymentId);
        } else {
            throw new BusinessException(ErrorCode.REFUND_FAILED,
                    "Refund failed for payment: " + paymentId);
        }

        return payment;
    }

    @Transactional(readOnly = true)
    public Payment getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Payment not found: " + paymentId));
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Payment not found for booking: " + bookingId));
    }
}
