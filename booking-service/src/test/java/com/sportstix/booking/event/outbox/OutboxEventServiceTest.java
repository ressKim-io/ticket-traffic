package com.sportstix.booking.event.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportstix.common.event.BookingEvent;
import com.sportstix.common.event.Topics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OutboxEventService outboxEventService;

    @Test
    void save_persistsOutboxEventWithSerializedPayload() {
        BookingEvent event = BookingEvent.created(1L, 100L, 10L, 200L, BigDecimal.valueOf(50000));

        outboxEventService.save("Booking", "1",
                "BOOKING_CREATED", Topics.BOOKING_CREATED, "10", event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getAggregateType()).isEqualTo("Booking");
        assertThat(saved.getAggregateId()).isEqualTo("1");
        assertThat(saved.getEventType()).isEqualTo("BOOKING_CREATED");
        assertThat(saved.getTopic()).isEqualTo(Topics.BOOKING_CREATED);
        assertThat(saved.getPartitionKey()).isEqualTo("10");
        assertThat(saved.getPayload()).contains("bookingId");
        assertThat(saved.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);
    }
}
