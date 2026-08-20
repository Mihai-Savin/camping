package com.camp.reservations.notification;

import java.util.List;

/**
 * Result of a reservation notification attempt, broken down per channel and
 * recipient so callers can tell the guest exactly what didn't go out (e.g.
 * "email to the campsite owner failed") instead of a single yes/no.
 */
public record NotificationOutcome(boolean allSucceeded, List<NotificationFailure> failures) {

    public static final NotificationOutcome SUCCESS = new NotificationOutcome(true, List.of());

    /** The service couldn't be reached/wasn't configured at all - no per-item breakdown is available. */
    public static NotificationOutcome unreachable() {
        return new NotificationOutcome(false,
                List.of(new NotificationFailure(null, null, "the notification service could not be reached")));
    }
}
