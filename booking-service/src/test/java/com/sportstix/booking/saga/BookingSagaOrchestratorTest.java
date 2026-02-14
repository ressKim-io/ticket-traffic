package com.sportstix.booking.saga;

import com.sportstix.booking.event.IdempotencyService;
import com.sportstix.common.event.PaymentEvent;
import com.sportstix.common.event.Topics;
import com.sportstix.common.exception.BusinessException;
import com.sportstix.common.response.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingSagaOrchestratorTest {

    @Mock
    private BookingSagaStep bookingSagaStep;
    @Mock
    private CompensationHandler compensationHandler;
    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private BookingSagaOrchestrator orchestrator;

    @Test
    void onPaymentCompleted_success_confirmsBooking() {
        PaymentEvent event = PaymentEvent.completed(1L, 10L, 100L, BigDecimal.valueOf(50000));
        given(idempotencyService.isDuplicate(event.getEventId(), Topics.PAYMENT_COMPLETED)).willReturn(false);

        orchestrator.onPaymentCompleted(event);

        verify(bookingSagaStep).confirm(10L);
        verify(compensationHandler, never()).compensate(anyLong(), anyString());
        verify(idempotencyService).markProcessed(event.getEventId(), Topics.PAYMENT_COMPLETED);
    }

    @Test
    void onPaymentCompleted_confirmFails_triggersCompensation() {
        PaymentEvent event = PaymentEvent.completed(1L, 10L, 100L, BigDecimal.valueOf(50000));
        given(idempotencyService.isDuplicate(event.getEventId(), Topics.PAYMENT_COMPLETED)).willReturn(false);

        doThrow(new BusinessException(ErrorCode.BOOKING_EXPIRED, "Expired"))
                .when(bookingSagaStep).confirm(10L);

        orchestrator.onPaymentCompleted(event);

        verify(bookingSagaStep).confirm(10L);
        verify(compensationHandler).compensate(eq(10L), contains("Confirmation failed"));
        verify(idempotencyService, never()).markProcessed(any(), any());
    }

    @Test
    void onPaymentFailed_compensatesBooking() {
        PaymentEvent event = PaymentEvent.failed(1L, 10L, 100L, BigDecimal.valueOf(50000));
        given(idempotencyService.isDuplicate(event.getEventId(), Topics.PAYMENT_FAILED)).willReturn(false);

        orchestrator.onPaymentFailed(event);

        verify(compensationHandler).compensate(10L, "Payment failed");
        verify(bookingSagaStep, never()).confirm(anyLong());
        verify(idempotencyService).markProcessed(event.getEventId(), Topics.PAYMENT_FAILED);
    }

    @Test
    void onPaymentRefunded_compensatesBooking() {
        PaymentEvent event = PaymentEvent.refunded(1L, 10L, 100L, BigDecimal.valueOf(50000));
        given(idempotencyService.isDuplicate(event.getEventId(), Topics.PAYMENT_REFUNDED)).willReturn(false);

        orchestrator.onPaymentRefunded(event);

        verify(compensationHandler).compensate(10L, "Payment refunded");
        verify(bookingSagaStep, never()).confirm(anyLong());
        verify(idempotencyService).markProcessed(event.getEventId(), Topics.PAYMENT_REFUNDED);
    }

    @Test
    void onPaymentCompleted_duplicate_skipped() {
        PaymentEvent event = PaymentEvent.completed(1L, 10L, 100L, BigDecimal.valueOf(50000));
        given(idempotencyService.isDuplicate(event.getEventId(), Topics.PAYMENT_COMPLETED)).willReturn(true);

        orchestrator.onPaymentCompleted(event);

        verify(bookingSagaStep, never()).confirm(anyLong());
        verify(compensationHandler, never()).compensate(anyLong(), anyString());
    }

    @Test
    void onPaymentFailed_duplicate_skipped() {
        PaymentEvent event = PaymentEvent.failed(1L, 10L, 100L, BigDecimal.valueOf(50000));
        given(idempotencyService.isDuplicate(event.getEventId(), Topics.PAYMENT_FAILED)).willReturn(true);

        orchestrator.onPaymentFailed(event);

        verify(compensationHandler, never()).compensate(anyLong(), anyString());
    }

    @Test
    void onPaymentRefunded_duplicate_skipped() {
        PaymentEvent event = PaymentEvent.refunded(1L, 10L, 100L, BigDecimal.valueOf(50000));
        given(idempotencyService.isDuplicate(event.getEventId(), Topics.PAYMENT_REFUNDED)).willReturn(true);

        orchestrator.onPaymentRefunded(event);

        verify(compensationHandler, never()).compensate(anyLong(), anyString());
    }
}
