package com.camp.reservations.notification;

import com.camp.reservations.domain.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Single entry point for reservation-creation notifications - fans out to
 * every configured channel (currently email and SMS). Callers only need to
 * know "notify about this reservation," not which channels exist or how each
 * is wired; adding a new channel later only touches this class, not
 * ReservationService.
 */
@Component
@RequiredArgsConstructor
public class ReservationNotificationService {

    private final ReservationEmailService reservationEmailService;
    private final ReservationSmsService reservationSmsService;

    public void notifyReservationCreated(Reservation reservation) {
        reservationEmailService.sendReservationNotification(reservation);
        reservationSmsService.sendReservationNotifications(reservation);
    }
}
