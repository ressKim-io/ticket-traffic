package com.sportstix.booking.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @InjectMocks
    private IdempotencyService idempotencyService;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Test
    void isDuplicate_eventExists_returnsTrue() {
        given(processedEventRepository.existsById("event-1")).willReturn(true);

        assertThat(idempotencyService.isDuplicate("event-1", "topic")).isTrue();
    }

    @Test
    void isDuplicate_eventNotExists_returnsFalse() {
        given(processedEventRepository.existsById("event-1")).willReturn(false);

        assertThat(idempotencyService.isDuplicate("event-1", "topic")).isFalse();
    }

    @Test
    void isDuplicate_nullEventId_returnsFalse() {
        assertThat(idempotencyService.isDuplicate(null, "topic")).isFalse();
        verify(processedEventRepository, never()).existsById(any());
    }

    @Test
    void markProcessed_savesEvent() {
        idempotencyService.markProcessed("event-1", "topic");

        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void markProcessed_duplicateInsert_ignoredGracefully() {
        doThrow(new DataIntegrityViolationException("Duplicate"))
                .when(processedEventRepository).save(any(ProcessedEvent.class));

        idempotencyService.markProcessed("event-1", "topic");

        verify(processedEventRepository).save(any(ProcessedEvent.class));
        // no exception thrown
    }

    @Test
    void markProcessed_nullEventId_noOp() {
        idempotencyService.markProcessed(null, "topic");

        verify(processedEventRepository, never()).save(any());
    }
}
