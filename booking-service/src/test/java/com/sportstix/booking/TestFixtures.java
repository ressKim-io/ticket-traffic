package com.sportstix.booking;

import com.sportstix.booking.domain.Booking;
import com.sportstix.booking.domain.BookingStatus;

import java.lang.reflect.Field;
import java.math.BigDecimal;

/**
 * Shared test utility for creating entities with preset IDs.
 * Uses reflection because JPA @Id fields have no public setter.
 */
public final class TestFixtures {

    private TestFixtures() {}

    public static void setEntityId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set ID on " + entity.getClass().getSimpleName(), e);
        }
    }

    public static Booking createBookingWithId(Long id, Long userId, Long gameId) {
        Booking booking = Booking.builder().userId(userId).gameId(gameId).build();
        setEntityId(booking, id);
        return booking;
    }

    public static Booking createBookingWithSeat(Long bookingId, Long seatId,
                                                 Long userId, Long gameId,
                                                 BookingStatus targetStatus) {
        Booking booking = createBookingWithId(bookingId, userId, gameId);
        booking.addSeat(seatId, BigDecimal.valueOf(50000));
        if (targetStatus == BookingStatus.CONFIRMED) {
            booking.confirm();
        } else if (targetStatus == BookingStatus.CANCELLED) {
            booking.cancel();
        }
        return booking;
    }
}
