package com.camp.reservations.notification;

/**
 * One failed delivery attempt reported by campsite-notifications. Channel and
 * recipientRole are null when the failure couldn't be attributed to a specific
 * channel/recipient (e.g. the service itself was unreachable, so there's no
 * per-item breakdown to read from).
 */
public record NotificationFailure(String channel, String recipientRole, String detail) {
}
