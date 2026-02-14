package com.sportstix.booking.repository;

import com.sportstix.booking.domain.Booking;
import com.sportstix.booking.domain.BookingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByStatusAndHoldExpiresAtBefore(
            BookingStatus status, LocalDateTime expiry, Pageable pageable);

    long countByUserIdAndGameIdAndStatusIn(Long userId, Long gameId, List<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b JOIN FETCH b.bookingSeats WHERE b.userId = :userId ORDER BY b.createdAt DESC")
    List<Booking> findByUserIdWithSeats(@Param("userId") Long userId);

    @Query("SELECT b FROM Booking b JOIN FETCH b.bookingSeats WHERE b.id = :id")
    Optional<Booking> findByIdWithSeats(@Param("id") Long id);
}
