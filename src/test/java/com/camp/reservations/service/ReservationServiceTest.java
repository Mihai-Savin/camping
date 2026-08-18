package com.camp.reservations.service;

import com.camp.reservations.domain.Campsite;
import com.camp.reservations.domain.Owner;
import com.camp.reservations.domain.Reservation;
import com.camp.reservations.domain.ReservationStatus;
import com.camp.reservations.dto.ReservationRequest;
import com.camp.reservations.exception.InvalidReservationException;
import com.camp.reservations.exception.ReservationConflictException;
import com.camp.reservations.repository.CampsiteRepository;
import com.camp.reservations.repository.OwnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private CampsiteRepository campsiteRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    private Campsite campsite;

    @BeforeEach
    void setUp() {
        Owner owner = ownerRepository.save(Owner.builder()
                .email("owner" + System.nanoTime() + "@example.com")
                .password("hashed")
                .displayName("Test Owner")
                .build());

        campsite = campsiteRepository.save(Campsite.builder()
                .name("Test Site " + System.nanoTime())
                .description("A site for testing")
                .capacity(4)
                .pricePerNight(new BigDecimal("25.00"))
                .active(true)
                .owner(owner)
                .build());
    }

    private ReservationRequest requestFor(LocalDate checkIn, LocalDate checkOut, int guests) {
        return new ReservationRequest(campsite.getId(), "Ada Lovelace", "ada@example.com",
                "555-1234", checkIn, checkOut, guests, null);
    }

    @Test
    void createsReservationWithCorrectTotalPrice() {
        Reservation reservation = reservationService.create(
                requestFor(LocalDate.now().plusDays(10), LocalDate.now().plusDays(13), 2));

        assertThat(reservation.getId()).isNotNull();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.getTotalPrice()).isEqualByComparingTo("75.00");
    }

    @Test
    void rejectsReservationExceedingCapacity() {
        assertThatThrownBy(() -> reservationService.create(
                requestFor(LocalDate.now().plusDays(5), LocalDate.now().plusDays(6), 10)))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("capacity");
    }

    @Test
    void rejectsCheckOutNotAfterCheckIn() {
        LocalDate day = LocalDate.now().plusDays(5);
        assertThatThrownBy(() -> reservationService.create(requestFor(day, day, 2)))
                .isInstanceOf(InvalidReservationException.class);
    }

    @Test
    void rejectsOverlappingDateRanges() {
        reservationService.create(
                requestFor(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), 2));

        assertThatThrownBy(() -> reservationService.create(
                requestFor(LocalDate.now().plusDays(3), LocalDate.now().plusDays(7), 2)))
                .isInstanceOf(ReservationConflictException.class);
    }

    @Test
    void allowsBackToBackReservationsWithNoOverlap() {
        reservationService.create(
                requestFor(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), 2));

        Reservation second = reservationService.create(
                requestFor(LocalDate.now().plusDays(5), LocalDate.now().plusDays(8), 2));

        assertThat(second.getId()).isNotNull();
    }

    @Test
    void cancellingAReservationFreesUpTheDates() {
        Reservation first = reservationService.create(
                requestFor(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), 2));

        reservationService.cancel(first.getId());

        Reservation second = reservationService.create(
                requestFor(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), 2));

        assertThat(second.getId()).isNotNull();
        assertThat(reservationService.findById(first.getId()).getStatus())
                .isEqualTo(ReservationStatus.CANCELLED);
    }
}
