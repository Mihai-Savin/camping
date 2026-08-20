package com.camp.reservations.service;

import com.camp.reservations.domain.Reservation;
import com.camp.reservations.notification.NotificationOutcome;

/**
 * The booking itself always succeeds independently of notifications - this
 * just lets callers also know exactly which notifications went out, so they
 * can warn the guest with specifics if some didn't without treating it as a
 * reservation failure.
 */
public record ReservationCreationResult(Reservation reservation, NotificationOutcome notificationOutcome) {
}
