package com.camp.reservations.repository;

import com.camp.reservations.domain.Reservation;
import com.camp.reservations.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("select r from Reservation r join fetch r.campsite where r.id = :id")
    Optional<Reservation> findByIdWithCampsite(@Param("id") Long id);

    @Query("select r from Reservation r join fetch r.campsite order by r.checkIn asc")
    List<Reservation> findAllByOrderByCheckInAsc();

    @Query("select r from Reservation r join fetch r.campsite where r.campsite.id = :campsiteId order by r.checkIn asc")
    List<Reservation> findByCampsiteIdOrderByCheckInAsc(@Param("campsiteId") Long campsiteId);

    @Query("select r from Reservation r join fetch r.campsite where upper(r.guestEmail) = upper(:guestEmail) order by r.checkIn desc")
    List<Reservation> findByGuestEmailIgnoreCaseOrderByCheckInDesc(@Param("guestEmail") String guestEmail);

    @Query("select r from Reservation r join fetch r.campsite where r.campsite.owner.id = :ownerId order by r.checkIn asc")
    List<Reservation> findByCampsiteOwnerIdOrderByCheckInAsc(@Param("ownerId") Long ownerId);

    /**
     * Two ranges [checkIn, checkOut) overlap when existing.checkIn < requested checkout
     * AND existing.checkOut > requested checkin. Cancelled reservations don't block a slot.
     */
    @Query("""
        select r from Reservation r
        where r.campsite.id = :campsiteId
          and r.status <> :cancelled
          and r.checkIn < :checkOut
          and r.checkOut > :checkIn
        """)
    List<Reservation> findOverlapping(@Param("campsiteId") Long campsiteId,
                                       @Param("checkIn") LocalDate checkIn,
                                       @Param("checkOut") LocalDate checkOut,
                                       @Param("cancelled") ReservationStatus cancelled);
}
