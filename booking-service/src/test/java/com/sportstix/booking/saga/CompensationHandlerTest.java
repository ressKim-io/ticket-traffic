package com.sportstix.booking.saga;

import com.sportstix.booking.service.BookingTransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompensationHandlerTest {

    @Mock
    private BookingTransactionService transactionService;

    @InjectMocks
    private CompensationHandler compensationHandler;

    @Test
    void compensate_success_releasesBooking() {
        compensationHandler.compensate(1L, "Payment failed");

        verify(transactionService).releaseBookingById(1L);
    }

    @Test
    void compensate_exceptionSwallowed_doesNotPropagate() {
        doThrow(new RuntimeException("DB error"))
                .when(transactionService).releaseBookingById(1L);

        // Should not throw
        compensationHandler.compensate(1L, "Payment failed");

        verify(transactionService).releaseBookingById(1L);
    }
}
