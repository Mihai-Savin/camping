package com.camp.reservations.dto;

import com.camp.reservations.domain.Reservation;
import com.camp.reservations.domain.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReservationResponse(
        Long id,
        Long campsiteId,
        String campsiteName,
        String guestName,
        String guestEmail,
        String guestPhone,
        LocalDate checkIn,
        LocalDate checkOut,
        Integer numberOfGuests,
        ReservationStatus status,
        BigDecimal totalPrice,
        String notes,
        Instant createdAt
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getCampsite().getId(),
                r.getCampsite().getName(),
                r.getGuestName(),
                r.getGuestEmail(),
                r.getGuestPhone(),
                r.getCheckIn(),
                r.getCheckOut(),
                r.getNumberOfGuests(),
                r.getStatus(),
                r.getTotalPrice(),
                r.getNotes(),
                r.getCreatedAt()
        );
    }
}
