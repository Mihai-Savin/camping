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

    @NotBlank(message = "Numele este obligatoriu")
    private String guestName;

    @NotBlank(message = "Adresa de email este obligatorie")
    @Email(message = "Adresa de email nu este validă")
    private String guestEmail;

    private String guestPhone;

    @NotNull(message = "Data de sosire este obligatorie")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkIn;

    @NotNull(message = "Data de plecare este obligatorie")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOut;

    @NotNull(message = "Numărul de oaspeți este obligatoriu")
    @Min(value = 1, message = "Trebuie să fie cel puțin 1 oaspete")
    private Integer numberOfGuests = 1;

    private String notes;

    public ReservationRequest toRequest() {
        return new ReservationRequest(campsiteId, guestName, guestEmail, guestPhone,
                checkIn, checkOut, numberOfGuests, notes);
    }
}
