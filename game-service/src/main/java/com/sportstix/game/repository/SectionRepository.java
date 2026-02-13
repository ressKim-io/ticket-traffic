package com.sportstix.game.repository;

import com.sportstix.game.domain.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByStadiumId(Long stadiumId);
}
