package com.sportstix.game.service;

import com.sportstix.common.exception.BusinessException;
import com.sportstix.game.domain.*;
import com.sportstix.game.dto.request.CreateGameRequest;
import com.sportstix.game.dto.response.GameDetailResponse;
import com.sportstix.game.dto.response.GameResponse;
import com.sportstix.game.dto.response.GameSeatResponse;
import com.sportstix.game.event.producer.GameEventProducer;
import com.sportstix.game.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @InjectMocks
    private GameService gameService;

    @Mock
    private GameRepository gameRepository;
    @Mock
    private GameSeatRepository gameSeatRepository;
    @Mock
    private StadiumRepository stadiumRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private GameEventProducer gameEventProducer;

    @Test
    void createGame_initializesSeatsFromStadium() {
        // given
        Stadium stadium = Stadium.builder()
                .name("Test Stadium")
                .address("123 Main St")
                .totalCapacity(100)
                .build();
        Section section = Section.builder()
                .name("A Block")
                .grade(SeatGrade.VIP)
                .capacity(50)
                .stadium(stadium)
                .build();
        stadium.addSection(section);

        Seat seat1 = Seat.builder().rowNumber("1").seatNumber(1).section(section).build();
        Seat seat2 = Seat.builder().rowNumber("1").seatNumber(2).section(section).build();

        CreateGameRequest request = new CreateGameRequest(
                1L, "Home", "Away",
                LocalDateTime.now().plusDays(7),
                LocalDateTime.now().plusDays(1),
                4
        );

        given(stadiumRepository.findByIdWithSections(1L)).willReturn(Optional.of(stadium));
        given(gameRepository.save(any(Game.class))).willAnswer(inv -> inv.getArgument(0));
        given(seatRepository.findBySectionIdIn(anyCollection())).willReturn(List.of(seat1, seat2));
        given(gameSeatRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

        // when
        GameResponse response = gameService.createGame(request);

        // then
        assertThat(response.homeTeam()).isEqualTo("Home");
        assertThat(response.awayTeam()).isEqualTo("Away");
        assertThat(response.status()).isEqualTo("SCHEDULED");

        ArgumentCaptor<List<GameSeat>> captor = ArgumentCaptor.forClass(List.class);
        verify(gameSeatRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);

        verify(gameEventProducer).publishSeatInitialized(any(Game.class), anyList());
    }

    @Test
    void createGame_stadiumNotFound_throwsException() {
        CreateGameRequest request = new CreateGameRequest(
                999L, "Home", "Away",
                LocalDateTime.now().plusDays(7),
                LocalDateTime.now().plusDays(1),
                4
        );

        given(stadiumRepository.findByIdWithSections(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.createGame(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getGame_returnsDetailWithSections() {
        Stadium stadium = Stadium.builder()
                .name("Test Stadium")
                .address("Addr")
                .totalCapacity(1000)
                .build();
        Game game = Game.builder()
                .stadium(stadium)
                .homeTeam("Home")
                .awayTeam("Away")
                .gameDate(LocalDateTime.now().plusDays(7))
                .ticketOpenAt(LocalDateTime.now().plusDays(1))
                .build();

        given(gameRepository.findByIdWithStadium(1L)).willReturn(Optional.of(game));
        List<Object[]> sectionRows = new ArrayList<>();
        sectionRows.add(new Object[]{1L, "A Block", SeatGrade.VIP, 50L, 48L});
        given(gameSeatRepository.countSeatsBySection(eq(1L), any(GameSeatStatus.class))).willReturn(sectionRows);

        GameDetailResponse result = gameService.getGame(1L);

        assertThat(result.homeTeam()).isEqualTo("Home");
        assertThat(result.stadium().name()).isEqualTo("Test Stadium");
        assertThat(result.sections()).hasSize(1);
        assertThat(result.sections().get(0).totalSeats()).isEqualTo(50);
        assertThat(result.sections().get(0).availableSeats()).isEqualTo(48);
    }

    @Test
    void getGame_notFound_throwsException() {
        given(gameRepository.findByIdWithStadium(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.getGame(999L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getGames_returnsPagedResults() {
        Stadium stadium = Stadium.builder()
                .name("Stadium")
                .address("Addr")
                .totalCapacity(1000)
                .build();
        Game game = Game.builder()
                .stadium(stadium)
                .homeTeam("Home")
                .awayTeam("Away")
                .gameDate(LocalDateTime.now().plusDays(7))
                .ticketOpenAt(LocalDateTime.now().plusDays(1))
                .build();

        PageRequest pageable = PageRequest.of(0, 20);
        given(gameRepository.findGamesWithFilter(null, null, null, null, pageable))
                .willReturn(new PageImpl<>(List.of(game), pageable, 1));

        Page<GameResponse> result = gameService.getGames(null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).homeTeam()).isEqualTo("Home");
    }

    @Test
    void getGameSeats_returnsSeatsForSection() {
        Section section = Section.builder()
                .name("A Block")
                .grade(SeatGrade.VIP)
                .capacity(50)
                .stadium(Stadium.builder().name("S").address("A").totalCapacity(100).build())
                .build();
        Seat seat = Seat.builder().rowNumber("1").seatNumber(1).section(section).build();
        Game game = Game.builder()
                .stadium(section.getStadium())
                .homeTeam("H")
                .awayTeam("A")
                .gameDate(LocalDateTime.now().plusDays(7))
                .ticketOpenAt(LocalDateTime.now().plusDays(1))
                .build();
        GameSeat gameSeat = GameSeat.builder().game(game).seat(seat).price(BigDecimal.valueOf(150000)).build();

        given(gameRepository.existsById(1L)).willReturn(true);
        given(gameSeatRepository.findByGameIdAndSectionId(1L, 1L)).willReturn(List.of(gameSeat));

        List<GameSeatResponse> result = gameService.getGameSeats(1L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).price()).isEqualTo(BigDecimal.valueOf(150000));
        assertThat(result.get(0).status()).isEqualTo("AVAILABLE");
    }

    @Test
    void getGameSeats_gameNotFound_throwsException() {
        given(gameRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> gameService.getGameSeats(999L, 1L))
                .isInstanceOf(BusinessException.class);
    }
}
