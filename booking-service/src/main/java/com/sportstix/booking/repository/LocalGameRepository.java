package com.sportstix.booking.repository;

import com.sportstix.booking.domain.LocalGame;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalGameRepository extends JpaRepository<LocalGame, Long> {
}
