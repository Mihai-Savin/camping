package com.camp.reservations.web.api;

import com.camp.reservations.dto.ReservationRequest;
import com.camp.reservations.dto.ReservationResponse;
import com.camp.reservations.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationRestController {

    private final ReservationService reservationService;

    @GetMapping
    public List<ReservationResponse> list(@RequestParam(required = false) Long campsiteId,
                                           @RequestParam(required = false) String guestEmail) {
        var reservations = campsiteId != null ? reservationService.findByCampsite(campsiteId)
                : guestEmail != null ? reservationService.findByGuestEmail(guestEmail)
                : reservationService.findAll();
        return reservations.stream().map(ReservationResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ReservationResponse get(@PathVariable Long id) {
        return ReservationResponse.from(reservationService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@Valid @RequestBody ReservationRequest request) {
        return ReservationResponse.from(reservationService.create(request).reservation());
    }

    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable Long id) {
        return ReservationResponse.from(reservationService.cancel(id));
    }
}
