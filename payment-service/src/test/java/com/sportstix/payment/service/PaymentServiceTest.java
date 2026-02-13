package com.sportstix.payment.service;

import com.sportstix.common.exception.BusinessException;
import com.sportstix.payment.domain.LocalBooking;
import com.sportstix.payment.domain.Payment;
import com.sportstix.payment.domain.PaymentStatus;
import com.sportstix.payment.event.producer.PaymentEventProducer;
import com.sportstix.payment.repository.LocalBookingRepository;
import com.sportstix.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private LocalBookingRepository localBookingRepository;
    @Mock
    private MockPgClient mockPgClient;
    @Mock
    private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void processPayment_success_completesPaymentAndPublishesEvent() {
        Long bookingId = 1L;
        LocalBooking booking = new LocalBooking(bookingId, 100L, 10L, "PENDING", BigDecimal.valueOf(50000));

        when(paymentRepository.existsByBookingIdAndStatusIn(eq(bookingId), any())).thenReturn(false);
        when(localBookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mockPgClient.charge(eq(bookingId), any()))
                .thenReturn(MockPgClient.PgResult.success("PG-ABCD1234"));

        Payment result = paymentService.processPayment(bookingId);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getPgTransactionId()).isEqualTo("PG-ABCD1234");
        verify(paymentEventProducer).publishCompleted(any(Payment.class));
        verify(paymentEventProducer, never()).publishFailed(any());
    }

    @Test
    void processPayment_pgFailure_failsPaymentAndPublishesEvent() {
        Long bookingId = 2L;
        LocalBooking booking = new LocalBooking(bookingId, 100L, 10L, "PENDING", BigDecimal.valueOf(30000));

        when(paymentRepository.existsByBookingIdAndStatusIn(eq(bookingId), any())).thenReturn(false);
        when(localBookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mockPgClient.charge(eq(bookingId), any()))
                .thenReturn(MockPgClient.PgResult.failure("Card declined"));

        Payment result = paymentService.processPayment(bookingId);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("Card declined");
        verify(paymentEventProducer).publishFailed(any(Payment.class));
        verify(paymentEventProducer, never()).publishCompleted(any());
    }

    @Test
    void processPayment_duplicatePayment_throwsException() {
        Long bookingId = 3L;
        when(paymentRepository.existsByBookingIdAndStatusIn(eq(bookingId),
                eq(List.of(PaymentStatus.PENDING, PaymentStatus.COMPLETED)))).thenReturn(true);

        assertThatThrownBy(() -> paymentService.processPayment(bookingId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Payment already exists");

        verify(mockPgClient, never()).charge(any(), any());
    }

    @Test
    void processPayment_bookingNotFound_throwsException() {
        Long bookingId = 4L;
        when(paymentRepository.existsByBookingIdAndStatusIn(eq(bookingId), any())).thenReturn(false);
        when(localBookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(bookingId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Booking not found");
    }

    @Test
    void processPayment_noPriceOnBooking_throwsException() {
        Long bookingId = 5L;
        LocalBooking booking = new LocalBooking(bookingId, 100L, 10L, "PENDING", null);

        when(paymentRepository.existsByBookingIdAndStatusIn(eq(bookingId), any())).thenReturn(false);
        when(localBookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.processPayment(bookingId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("total price not available");
    }

    @Test
    void refundPayment_success_refundsAndPublishesEvent() {
        Payment payment = Payment.builder()
                .bookingId(1L).userId(100L).amount(BigDecimal.valueOf(50000)).build();
        payment.complete("PG-ABCD1234");

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mockPgClient.refund(eq("PG-ABCD1234"), any()))
                .thenReturn(MockPgClient.PgResult.success("RF-XXXX1234"));

        Payment result = paymentService.refundPayment(1L);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentEventProducer).publishRefunded(any(Payment.class));
    }

    @Test
    void refundPayment_pgFails_throwsException() {
        Payment payment = Payment.builder()
                .bookingId(1L).userId(100L).amount(BigDecimal.valueOf(50000)).build();
        payment.complete("PG-ABCD1234");

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(mockPgClient.refund(eq("PG-ABCD1234"), any()))
                .thenReturn(MockPgClient.PgResult.failure("Refund timeout"));

        assertThatThrownBy(() -> paymentService.refundPayment(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Refund failed");
    }

    @Test
    void refundPayment_notFound_throwsException() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.refundPayment(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    void getPayment_found_returnsPayment() {
        Payment payment = Payment.builder()
                .bookingId(1L).userId(100L).amount(BigDecimal.valueOf(50000)).build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        Payment result = paymentService.getPayment(1L);

        assertThat(result.getBookingId()).isEqualTo(1L);
    }

    @Test
    void getPaymentByBookingId_found_returnsPayment() {
        Payment payment = Payment.builder()
                .bookingId(1L).userId(100L).amount(BigDecimal.valueOf(50000)).build();
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));

        Payment result = paymentService.getPaymentByBookingId(1L);

        assertThat(result.getBookingId()).isEqualTo(1L);
    }
}
