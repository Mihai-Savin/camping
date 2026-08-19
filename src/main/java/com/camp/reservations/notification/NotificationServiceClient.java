package com.camp.reservations.notification;

import com.camp.reservations.domain.Reservation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Calls the standalone campsite-notifications microservice instead of
 * emailing/texting inline. Same safety contract as the integrations it
 * replaced: missing config or a downstream failure is logged and swallowed,
 * never thrown - a notification outage must never block or roll back a
 * reservation.
 */
@Slf4j
@Component
public class NotificationServiceClient {

    private final RestClient client;
    private final String apiKey;
    private final boolean configured;

    public NotificationServiceClient(@Value("${notifications.service-url:}") String baseUrl,
                                      @Value("${notifications.service-api-key:}") String apiKey) {
        this.configured = StringUtils.hasText(baseUrl) && StringUtils.hasText(apiKey);
        this.client = RestClient.builder().baseUrl(configured ? baseUrl : "http://unset").build();
        this.apiKey = apiKey;
    }

    public void notifyBookingRequest(Reservation reservation) {
        if (!configured) {
            log.info("campsite-notifications not configured; skipping notifications for reservation {}",
                    reservation.getId());
            return;
        }

        try {
            client.post()
                    .uri("/api/notifications/booking-request")
                    .header("X-API-Key", apiKey)
                    .body(toPayload(reservation))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent booking-request notification for reservation {}", reservation.getId());
        } catch (RestClientException ex) {
            log.warn("Failed to notify campsite-notifications for reservation {}", reservation.getId(), ex);
        }
    }

    private BookingNotificationPayload toPayload(Reservation reservation) {
        var campsite = reservation.getCampsite();
        var owner = campsite.getOwner();

        return new BookingNotificationPayload(
                String.valueOf(reservation.getId()),
                new Recipient(reservation.getGuestName(), reservation.getGuestEmail(), reservation.getGuestPhone()),
                new Recipient(owner.getDisplayName(), owner.getEmail(), campsite.getPhone()),
                new BookingDetails(
                        campsite.getName(),
                        reservation.getCheckIn().toString(),
                        reservation.getCheckOut().toString(),
                        reservation.getNumberOfGuests(),
                        reservation.getTotalPrice() != null ? reservation.getTotalPrice().toString() : null,
                        reservation.getNotes()
                )
        );
    }

    private record BookingNotificationPayload(String bookingReference, Recipient guest, Recipient owner, BookingDetails booking) {
    }

    private record Recipient(String name, String email, String phone) {
    }

    private record BookingDetails(String campsiteName, String checkIn, String checkOut,
                                   Integer numberOfGuests, String totalPrice, String notes) {
    }
}
