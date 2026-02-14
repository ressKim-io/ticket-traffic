package com.sportstix.booking.repository;

import com.sportstix.booking.domain.Booking;
import com.sportstix.booking.domain.BookingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByStatusAndHoldExpiresAtBefore(
            BookingStatus status, LocalDateTime expiry, Pageable pageable);

    long countByUserIdAndGameIdAndStatusIn(Long userId, Long gameId, List<BookingStatus> statuses);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
}
