package com.sportstix.booking.saga;

import com.sportstix.common.event.PaymentEvent;
import com.sportstix.common.exception.BusinessException;
import com.sportstix.common.response.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingSagaOrchestratorTest {

    @Mock
    private BookingSagaStep bookingSagaStep;
    @Mock
    private CompensationHandler compensationHandler;

    @InjectMocks
    private BookingSagaOrchestrator orchestrator;

    @Test
    void onPaymentCompleted_success_confirmsBooking() {
        PaymentEvent event = PaymentEvent.completed(1L, 10L, 100L, BigDecimal.valueOf(50000));

        orchestrator.onPaymentCompleted(event);

        verify(bookingSagaStep).confirm(10L);
        verify(compensationHandler, never()).compensate(anyLong(), anyString());
    }

    @Test
    void onPaymentCompleted_confirmFails_triggersCompensation() {
        PaymentEvent event = PaymentEvent.completed(1L, 10L, 100L, BigDecimal.valueOf(50000));

        doThrow(new BusinessException(ErrorCode.BOOKING_EXPIRED, "Expired"))
                .when(bookingSagaStep).confirm(10L);

        orchestrator.onPaymentCompleted(event);

        verify(bookingSagaStep).confirm(10L);
        verify(compensationHandler).compensate(eq(10L), contains("Confirmation failed"));
    }

    @Test
    void onPaymentFailed_compensatesBooking() {
        PaymentEvent event = PaymentEvent.failed(1L, 10L, 100L, BigDecimal.valueOf(50000));

        orchestrator.onPaymentFailed(event);

        verify(compensationHandler).compensate(10L, "Payment failed");
        verify(bookingSagaStep, never()).confirm(anyLong());
    }

    @Test
    void onPaymentRefunded_compensatesBooking() {
        PaymentEvent event = PaymentEvent.refunded(1L, 10L, 100L, BigDecimal.valueOf(50000));

        orchestrator.onPaymentRefunded(event);

        verify(compensationHandler).compensate(10L, "Payment refunded");
        verify(bookingSagaStep, never()).confirm(anyLong());
    }
}
