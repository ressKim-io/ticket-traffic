package com.sportstix.game.repository;

import com.sportstix.game.domain.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findBySectionId(Long sectionId);

    int countBySectionId(Long sectionId);
}
