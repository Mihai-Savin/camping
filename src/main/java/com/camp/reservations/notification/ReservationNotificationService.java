package com.camp.reservations.notification;

import com.camp.reservations.domain.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Single entry point for reservation-creation notifications. Delegates to the
 * standalone campsite-notifications microservice, which fans out to email and
 * SMS for both the guest and the owner. Callers only need to know "notify
 * about this reservation," not how or where that happens.
 */
@Component
@RequiredArgsConstructor
public class ReservationNotificationService {

    private final NotificationServiceClient notificationServiceClient;

    public void notifyReservationCreated(Reservation reservation) {
        notificationServiceClient.notifyBookingRequest(reservation);
    }
}
