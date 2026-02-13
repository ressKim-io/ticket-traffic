package com.sportstix.booking.event.consumer;

import com.sportstix.booking.domain.LocalGame;
import com.sportstix.booking.domain.LocalGameSeat;
import com.sportstix.booking.repository.LocalGameRepository;
import com.sportstix.booking.repository.LocalGameSeatRepository;
import com.sportstix.common.event.GameInfoUpdatedEvent;
import com.sportstix.common.event.SeatInitializedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameEventConsumerTest {

    @InjectMocks
    private GameEventConsumer consumer;

    @Mock
    private LocalGameRepository localGameRepository;
    @Mock
    private LocalGameSeatRepository localGameSeatRepository;

    @Test
    void handleSeatInitialized_createsGameAndSeats() {
        SeatInitializedEvent event = new SeatInitializedEvent(
                1L, "Home", "Away",
                LocalDateTime.of(2025, 3, 15, 19, 0),
                LocalDateTime.of(2025, 3, 10, 10, 0),
                "SCHEDULED", 4,
                List.of(
                        new SeatInitializedEvent.SeatInfo(100L, 10L, 1L, BigDecimal.valueOf(50000), "A", 1),
                        new SeatInitializedEvent.SeatInfo(101L, 11L, 1L, BigDecimal.valueOf(50000), "A", 2)
                )
        );
        given(localGameRepository.findById(1L)).willReturn(Optional.empty());
        given(localGameRepository.save(any(LocalGame.class))).willAnswer(inv -> inv.getArgument(0));

        consumer.handleSeatInitialized(event);

        verify(localGameRepository).save(any(LocalGame.class));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LocalGameSeat>> seatCaptor = ArgumentCaptor.forClass(List.class);
        verify(localGameSeatRepository).saveAll(seatCaptor.capture());
        assertThat(seatCaptor.getValue()).hasSize(2);
    }

    @Test
    void handleSeatInitialized_existingGame_updates() {
        LocalGame existing = new LocalGame(1L, "Old Home", "Old Away",
                LocalDateTime.now(), LocalDateTime.now(), "SCHEDULED", 4);
        SeatInitializedEvent event = new SeatInitializedEvent(
                1L, "New Home", "New Away",
                LocalDateTime.of(2025, 3, 15, 19, 0),
                LocalDateTime.of(2025, 3, 10, 10, 0),
                "OPEN", 2, List.of()
        );
        given(localGameRepository.findById(1L)).willReturn(Optional.of(existing));
        given(localGameRepository.save(any(LocalGame.class))).willAnswer(inv -> inv.getArgument(0));

        consumer.handleSeatInitialized(event);

        verify(localGameRepository).save(existing);
        assertThat(existing.getHomeTeam()).isEqualTo("New Home");
        assertThat(existing.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void handleGameInfoUpdated_createsNewReplica() {
        GameInfoUpdatedEvent event = new GameInfoUpdatedEvent(
                2L, "TeamA", "TeamB",
                LocalDateTime.of(2025, 4, 1, 18, 0),
                LocalDateTime.of(2025, 3, 25, 10, 0),
                "SCHEDULED", 4
        );
        given(localGameRepository.findById(2L)).willReturn(Optional.empty());

        consumer.handleGameInfoUpdated(event);

        ArgumentCaptor<LocalGame> captor = ArgumentCaptor.forClass(LocalGame.class);
        verify(localGameRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(2L);
        assertThat(captor.getValue().getHomeTeam()).isEqualTo("TeamA");
    }

    @Test
    void handleGameInfoUpdated_updatesExisting() {
        LocalGame existing = new LocalGame(2L, "OldA", "OldB",
                LocalDateTime.now(), LocalDateTime.now(), "SCHEDULED", 4);
        GameInfoUpdatedEvent event = new GameInfoUpdatedEvent(
                2L, "NewA", "NewB",
                LocalDateTime.of(2025, 4, 1, 18, 0),
                LocalDateTime.of(2025, 3, 25, 10, 0),
                "OPEN", 2
        );
        given(localGameRepository.findById(2L)).willReturn(Optional.of(existing));

        consumer.handleGameInfoUpdated(event);

        verify(localGameRepository).save(existing);
        assertThat(existing.getHomeTeam()).isEqualTo("NewA");
        assertThat(existing.getMaxTicketsPerUser()).isEqualTo(2);
    }
}
