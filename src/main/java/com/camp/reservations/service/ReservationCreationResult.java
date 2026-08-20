package com.camp.reservations.service;

import com.camp.reservations.domain.Reservation;

/**
 * The booking itself always succeeds independently of notifications - this
 * just lets callers also know whether the guest/owner notification actually
 * went out, so they can warn the guest if it didn't without treating it as a
 * reservation failure.
 */
public record ReservationCreationResult(Reservation reservation, boolean notificationsSucceeded) {
}
