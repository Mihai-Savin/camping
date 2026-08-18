package com.camp.reservations.web.ui;

import com.camp.reservations.dto.ReservationRequest;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class ReservationForm {

    @NotNull
    private Long campsiteId;

    @NotBlank
    private String guestName;

    @NotBlank
    @Email
    private String guestEmail;

    private String guestPhone;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkIn;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOut;

    @NotNull
    @Min(1)
    private Integer numberOfGuests = 1;

    private String notes;

    public ReservationRequest toRequest() {
        return new ReservationRequest(campsiteId, guestName, guestEmail, guestPhone,
                checkIn, checkOut, numberOfGuests, notes);
    }
}
