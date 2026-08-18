package com.camp.reservations.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record ReservationRequest(
        @NotNull Long campsiteId,
        @NotBlank String guestName,
        @NotBlank @Email String guestEmail,
        String guestPhone,
        @NotNull @FutureOrPresent LocalDate checkIn,
        @NotNull @Future LocalDate checkOut,
        @NotNull @Min(1) Integer numberOfGuests,
        String notes
) {
}
