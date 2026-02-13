package com.sportstix.game.repository;

import com.sportstix.game.domain.GameSeat;
import com.sportstix.game.domain.GameSeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameSeatRepository extends JpaRepository<GameSeat, Long> {

    @Query("""
            SELECT gs FROM GameSeat gs
            JOIN FETCH gs.seat s
            JOIN FETCH s.section
            WHERE gs.game.id = :gameId
              AND s.section.id = :sectionId
            ORDER BY s.rowNumber, s.seatNumber
            """)
    List<GameSeat> findByGameIdAndSectionId(
            @Param("gameId") Long gameId,
            @Param("sectionId") Long sectionId
    );

    int countByGameIdAndStatus(Long gameId, GameSeatStatus status);

    @Query("""
            SELECT s.section.id, s.section.name, s.section.grade,
                   COUNT(gs), SUM(CASE WHEN gs.status = 'AVAILABLE' THEN 1 ELSE 0 END)
            FROM GameSeat gs
            JOIN gs.seat s
            WHERE gs.game.id = :gameId
            GROUP BY s.section.id, s.section.name, s.section.grade
            """)
    List<Object[]> countSeatsBySection(@Param("gameId") Long gameId);
}
