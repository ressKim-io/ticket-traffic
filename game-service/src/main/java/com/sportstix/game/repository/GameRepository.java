package com.sportstix.game.repository;

import com.sportstix.game.domain.Game;
import com.sportstix.game.domain.GameStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    @Query("SELECT g FROM Game g JOIN FETCH g.stadium WHERE g.id = :id")
    Optional<Game> findByIdWithStadium(@Param("id") Long id);

    @Query("""
            SELECT g FROM Game g JOIN FETCH g.stadium
            WHERE (:status IS NULL OR g.status = :status)
              AND (:teamName IS NULL OR g.homeTeam LIKE %:teamName% OR g.awayTeam LIKE %:teamName%)
              AND (:from IS NULL OR g.gameDate >= :from)
              AND (:to IS NULL OR g.gameDate <= :to)
            """)
    Page<Game> findGamesWithFilter(
            @Param("status") GameStatus status,
            @Param("teamName") String teamName,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}
