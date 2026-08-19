package com.camp.reservations.service;

import com.camp.reservations.domain.Campsite;
import com.camp.reservations.domain.Owner;
import com.camp.reservations.domain.Reservation;
import com.camp.reservations.domain.ReservationStatus;
import com.camp.reservations.dto.ReservationRequest;
import com.camp.reservations.exception.InvalidReservationException;
import com.camp.reservations.exception.ReservationConflictException;
import com.camp.reservations.exception.ResourceNotFoundException;
import com.camp.reservations.notification.ReservationEmailService;
import com.camp.reservations.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CampsiteService campsiteService;
    private final ReservationEmailService reservationEmailService;

    public List<Reservation> findAll() {
        return reservationRepository.findAllByOrderByCheckInAsc();
    }

    public Reservation findById(Long id) {
        return reservationRepository.findByIdWithCampsite(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
    }

    public List<Reservation> findByCampsite(Long campsiteId) {
        return reservationRepository.findByCampsiteIdOrderByCheckInAsc(campsiteId);
    }

    public List<Reservation> findByGuestEmail(String email) {
        return reservationRepository.findByGuestEmailIgnoreCaseOrderByCheckInDesc(email);
    }

    public List<Reservation> findByOwner(Owner owner) {
        return reservationRepository.findByCampsiteOwnerIdOrderByCheckInAsc(owner.getId());
    }

    @Transactional
    public Reservation create(ReservationRequest request) {
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new InvalidReservationException("Check-out date must be after check-in date");
        }

        Campsite campsite = campsiteService.findById(request.campsiteId());

        if (!campsite.isActive()) {
            throw new InvalidReservationException("Campsite '" + campsite.getName() + "' is not currently available");
        }

        if (request.numberOfGuests() > campsite.getCapacity()) {
            throw new InvalidReservationException(
                    "Campsite '" + campsite.getName() + "' has a capacity of " + campsite.getCapacity()
                            + " guests, but " + request.numberOfGuests() + " were requested");
        }

        List<Reservation> overlapping = reservationRepository.findOverlapping(
                campsite.getId(), request.checkIn(), request.checkOut(), ReservationStatus.CANCELLED);
        if (!overlapping.isEmpty()) {
            throw new ReservationConflictException(
                    "Campsite '" + campsite.getName() + "' is already booked for part of that date range");
        }

        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        BigDecimal totalPrice = campsite.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        Reservation reservation = Reservation.builder()
                .campsite(campsite)
                .guestName(request.guestName())
                .guestEmail(request.guestEmail())
                .guestPhone(request.guestPhone())
                .checkIn(request.checkIn())
                .checkOut(request.checkOut())
                .numberOfGuests(request.numberOfGuests())
                .status(ReservationStatus.CONFIRMED)
                .totalPrice(totalPrice)
                .notes(request.notes())
                .build();

        Reservation saved = reservationRepository.save(reservation);
        reservationEmailService.sendReservationNotification(saved);
        return saved;
    }

    @Transactional
    public Reservation cancel(Long id) {
        Reservation reservation = findById(id);
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new InvalidReservationException("Reservation is already cancelled");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        return reservation;
    }

    @Transactional
    public Reservation cancelAsOwner(Long id, Owner currentOwner) {
        Reservation reservation = findById(id);
        if (!reservation.getCampsite().getOwner().getId().equals(currentOwner.getId())) {
            throw new AccessDeniedException("You do not own the campsite for this reservation");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new InvalidReservationException("Reservation is already cancelled");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        return reservation;
    }
}
