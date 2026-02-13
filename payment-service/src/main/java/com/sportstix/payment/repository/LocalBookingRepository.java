package com.sportstix.payment.repository;

import com.sportstix.payment.domain.LocalBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocalBookingRepository extends JpaRepository<LocalBooking, Long> {

    List<LocalBooking> findByUserId(Long userId);

    List<LocalBooking> findByGameIdAndStatus(Long gameId, String status);
}
