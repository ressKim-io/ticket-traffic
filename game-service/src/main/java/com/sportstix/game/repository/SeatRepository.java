package com.sportstix.game.repository;

import com.sportstix.game.domain.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findBySectionId(Long sectionId);

    @Query("SELECT s FROM Seat s JOIN FETCH s.section WHERE s.section.id IN :sectionIds")
    List<Seat> findBySectionIdIn(@Param("sectionIds") Collection<Long> sectionIds);

    int countBySectionId(Long sectionId);
}
