package com.sportstix.booking.repository;

import com.sportstix.booking.domain.LocalGameSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LocalGameSeatRepository extends JpaRepository<LocalGameSeat, Long> {

    @Query("SELECT s FROM LocalGameSeat s WHERE s.gameId = :gameId AND s.sectionId = :sectionId")
    List<LocalGameSeat> findByGameIdAndSectionId(@Param("gameId") Long gameId,
                                                  @Param("sectionId") Long sectionId);

    @Query("SELECT s FROM LocalGameSeat s WHERE s.gameId = :gameId AND s.status = :status")
    List<LocalGameSeat> findByGameIdAndStatus(@Param("gameId") Long gameId,
                                               @Param("status") String status);

    @Query("SELECT COUNT(s) FROM LocalGameSeat s WHERE s.gameId = :gameId AND s.status = :status")
    long countByGameIdAndStatus(@Param("gameId") Long gameId,
                                @Param("status") String status);
}
