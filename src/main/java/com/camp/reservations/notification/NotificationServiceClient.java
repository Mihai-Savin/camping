package com.camp.reservations.notification;

import com.camp.reservations.domain.Reservation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Calls the standalone campsite-notifications microservice instead of
 * emailing/texting inline. Same safety contract as the integrations it
 * replaced: missing config or a downstream failure is logged and swallowed,
 * never thrown - a notification outage must never block or roll back a
 * reservation.
 *
 * <p>The service replies 200 even when individual deliveries fail (it
 * reports a per-channel "results" array with SENT/FAILED/SKIPPED statuses
 * rather than an HTTP error), so a bodiless retrieve would silently miss
 * partial failures. This parses that body and reports back whether every
 * attempted delivery actually succeeded, so callers can tell the guest their
 * booking is fine but the confirmation notification may not have gone out.
 *
 * <p>Explicit connect/read timeouts are set because this service runs on
 * Render's free tier too and can hit the same cold-start delay ours does
 * (observed up to ~100s) - without a bound, a slow or genuinely hung
 * downstream would block the booking request thread indefinitely, which
 * defeats the "never block a reservation" contract just as surely as
 * letting an exception propagate would.
 */
@Slf4j
@Component
public class NotificationServiceClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(120);

    private final RestClient client;
    private final String apiKey;
    private final boolean configured;

    public NotificationServiceClient(@Value("${notifications.service-url:}") String baseUrl,
                                      @Value("${notifications.service-api-key:}") String apiKey) {
        this.configured = StringUtils.hasText(baseUrl) && StringUtils.hasText(apiKey);

        var requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        requestFactory.setReadTimeout(READ_TIMEOUT);

        this.client = RestClient.builder()
                .baseUrl(configured ? baseUrl : "http://unset")
                .requestFactory(requestFactory)
                .build();
        this.apiKey = apiKey;
    }

    /**
     * @return true if notifications are confirmed sent (or intentionally skipped
     * with no failures reported), false if the service is unreachable, misconfigured,
     * or reported at least one failed delivery.
     */
    public boolean notifyBookingRequest(Reservation reservation) {
        if (!configured) {
            log.info("campsite-notifications not configured; skipping notifications for reservation {}",
                    reservation.getId());
            return false;
        }

        try {
            JsonNode response = client.post()
                    .uri("/api/notifications/booking-request")
                    .header("X-API-Key", apiKey)
                    .body(toPayload(reservation))
                    .retrieve()
                    .body(JsonNode.class);

            if (hasFailure(response)) {
                log.warn("campsite-notifications reported a failed delivery for reservation {}: {}",
                        reservation.getId(), response);
                return false;
            }
            log.info("Sent booking-request notification for reservation {}", reservation.getId());
            return true;
        } catch (RestClientException ex) {
            log.warn("Failed to notify campsite-notifications for reservation {}", reservation.getId(), ex);
            return false;
        }
    }

    private boolean hasFailure(JsonNode response) {
        if (response == null) {
            return true;
        }
        JsonNode results = response.path("results");
        if (!results.isArray()) {
            return false;
        }
        for (JsonNode result : results) {
            if ("FAILED".equalsIgnoreCase(result.path("status").asString(""))) {
                return true;
            }
        }
        return false;
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
