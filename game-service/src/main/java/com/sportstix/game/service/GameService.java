package com.sportstix.game.service;

import com.sportstix.common.exception.BusinessException;
import com.sportstix.common.response.ErrorCode;
import com.sportstix.game.domain.*;
import com.sportstix.game.dto.request.CreateGameRequest;
import com.sportstix.game.dto.response.GameDetailResponse;
import com.sportstix.game.dto.response.GameDetailResponse.SectionSeatSummary;
import com.sportstix.game.dto.response.GameResponse;
import com.sportstix.game.dto.response.GameSeatResponse;
import com.sportstix.game.event.producer.GameEventProducer;
import com.sportstix.game.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final GameSeatRepository gameSeatRepository;
    private final StadiumRepository stadiumRepository;
    private final SeatRepository seatRepository;
    private final GameEventProducer gameEventProducer;

    @Transactional
    public GameResponse createGame(CreateGameRequest request) {
        Stadium stadium = stadiumRepository.findByIdWithSections(request.stadiumId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Stadium not found: " + request.stadiumId()));

        Game game = Game.builder()
                .stadium(stadium)
                .homeTeam(request.homeTeam())
                .awayTeam(request.awayTeam())
                .gameDate(request.gameDate())
                .ticketOpenAt(request.ticketOpenAt())
                .maxTicketsPerUser(request.maxTicketsPerUser())
                .build();

        Game savedGame = gameRepository.save(game);

        int totalSeats = initializeGameSeats(savedGame, stadium);

        log.info("Created game id={} with {} seats at {}", savedGame.getId(), totalSeats, stadium.getName());

        gameEventProducer.publishSeatInitialized(savedGame.getId(), totalSeats);

        return GameResponse.from(savedGame);
    }

    @Transactional(readOnly = true)
    public GameDetailResponse getGame(Long gameId) {
        Game game = gameRepository.findByIdWithStadium(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND,
                        "Game not found: " + gameId));

        List<SectionSeatSummary> sections = gameSeatRepository.countSeatsBySection(gameId, GameSeatStatus.AVAILABLE)
                .stream()
                .map(row -> new SectionSeatSummary(
                        (Long) row[0],
                        (String) row[1],
                        ((SeatGrade) row[2]).name(),
                        (Long) row[3],
                        (Long) row[4]
                ))
                .toList();

        return GameDetailResponse.of(game, sections);
    }

    @Transactional(readOnly = true)
    public Page<GameResponse> getGames(GameStatus status, String teamName,
                                       LocalDateTime from, LocalDateTime to,
                                       Pageable pageable) {
        return gameRepository.findGamesWithFilter(status, teamName, from, to, pageable)
                .map(GameResponse::from);
    }

    @Transactional(readOnly = true)
    public List<GameSeatResponse> getGameSeats(Long gameId, Long sectionId) {
        if (!gameRepository.existsById(gameId)) {
            throw new BusinessException(ErrorCode.GAME_NOT_FOUND, "Game not found: " + gameId);
        }

        return gameSeatRepository.findByGameIdAndSectionId(gameId, sectionId)
                .stream()
                .map(GameSeatResponse::from)
                .toList();
    }

    private int initializeGameSeats(Game game, Stadium stadium) {
        List<Long> sectionIds = stadium.getSections().stream()
                .map(Section::getId)
                .toList();

        List<Seat> allSeats = seatRepository.findBySectionIdIn(sectionIds);

        List<GameSeat> gameSeats = allSeats.stream()
                .map(seat -> GameSeat.builder()
                        .game(game)
                        .seat(seat)
                        .price(seat.getSection().getGrade().getDefaultPrice())
                        .build())
                .toList();

        gameSeatRepository.saveAll(gameSeats);

        return gameSeats.size();
    }
}
