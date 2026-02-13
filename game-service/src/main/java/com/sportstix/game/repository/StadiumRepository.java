package com.sportstix.game.repository;

import com.sportstix.game.domain.Stadium;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface StadiumRepository extends JpaRepository<Stadium, Long> {

    @Query("SELECT s FROM Stadium s LEFT JOIN FETCH s.sections WHERE s.id = :id")
    Optional<Stadium> findByIdWithSections(Long id);
}
