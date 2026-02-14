package com.sportstix.payment.repository;

import com.sportstix.payment.domain.Payment;
import com.sportstix.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    boolean existsByBookingIdAndStatusIn(Long bookingId, List<PaymentStatus> statuses);
}
