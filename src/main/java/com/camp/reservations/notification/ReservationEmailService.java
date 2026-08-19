package com.camp.reservations.notification;

import com.camp.reservations.domain.Campsite;
import com.camp.reservations.domain.FacilityType;
import com.camp.reservations.domain.Reservation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Standalone notification component: emails both the campsite owner and the
 * guest whenever a new reservation is made - the owner gets the full booking
 * summary, the guest gets a confirmation. Deliberately isolated from the core
 * booking logic in ReservationService - a Resend outage or missing API key
 * must never block or roll back a reservation, so every failure here is
 * caught and logged, never thrown, and one recipient failing never stops the
 * other from being attempted.
 */
@Slf4j
@Component
public class ReservationEmailService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final RestClient resendClient;
    private final String apiKey;
    private final String fromEmail;

    public ReservationEmailService(@Value("${resend.api-key:}") String apiKey,
                                    @Value("${resend.from-email:onboarding@resend.dev}") String fromEmail) {
        this.resendClient = RestClient.builder().baseUrl("https://api.resend.com").build();
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
    }

    public void sendReservationNotification(Reservation reservation) {
        if (!StringUtils.hasText(apiKey)) {
            log.info("RESEND_API_KEY not configured; skipping reservation notification emails for reservation {}",
                    reservation.getId());
            return;
        }

        Campsite campsite = reservation.getCampsite();

        send(campsite.getOwner().getEmail(), "New reservation: " + campsite.getName(),
                buildOwnerHtml(reservation, campsite), reservation.getId(), "owner");

        send(reservation.getGuestEmail(), "Your reservation at " + campsite.getName() + " is confirmed",
                buildGuestHtml(reservation, campsite), reservation.getId(), "guest");
    }

    private void send(String to, String subject, String html, Long reservationId, String recipientLabel) {
        try {
            resendClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(Map.of(
                            "from", fromEmail,
                            "to", List.of(to),
                            "subject", subject,
                            "html", html
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent {} reservation notification email to {} for reservation {}", recipientLabel, to, reservationId);
        } catch (RestClientException ex) {
            log.warn("Failed to send {} reservation notification email for reservation {}", recipientLabel, reservationId, ex);
        }
    }

    private String buildOwnerHtml(Reservation reservation, Campsite campsite) {
        FacilityType facilityType = campsite.getFacilityType();
        String facilityLabel = facilityType != null ? capitalize(facilityType.name()) : "Not specified";
        String misc = StringUtils.hasText(reservation.getNotes()) ? reservation.getNotes() : "None";
        String phone = StringUtils.hasText(reservation.getGuestPhone()) ? reservation.getGuestPhone() : "Not provided";

        return """
                <h2>New reservation for %s</h2>
                <p><strong>Guest:</strong> %s (%s)</p>
                <p><strong>Phone:</strong> %s</p>
                <table cellpadding="6" style="border-collapse:collapse">
                  <tr><td><strong>Start date</strong></td><td>%s</td></tr>
                  <tr><td><strong>End date</strong></td><td>%s</td></tr>
                  <tr><td><strong>Facility type</strong></td><td>%s</td></tr>
                  <tr><td><strong>Number of guests</strong></td><td>%d</td></tr>
                  <tr><td><strong>Total price</strong></td><td>$%s</td></tr>
                </table>
                <p><strong>Notes / misc:</strong> %s</p>
                """.formatted(
                        escape(campsite.getName()),
                        escape(reservation.getGuestName()), escape(reservation.getGuestEmail()),
                        escape(phone),
                        reservation.getCheckIn().format(DATE_FORMAT),
                        reservation.getCheckOut().format(DATE_FORMAT),
                        facilityLabel,
                        reservation.getNumberOfGuests(),
                        reservation.getTotalPrice(),
                        escape(misc)
                );
    }

    private String buildGuestHtml(Reservation reservation, Campsite campsite) {
        String contactLine = StringUtils.hasText(campsite.getPhone())
                ? "<p>Questions? Contact the campsite at " + escape(campsite.getPhone()) + ".</p>"
                : "";

        return """
                <h2>Your reservation is confirmed</h2>
                <p>Thanks, %s! Here are your booking details for <strong>%s</strong>:</p>
                <table cellpadding="6" style="border-collapse:collapse">
                  <tr><td><strong>Check-in</strong></td><td>%s</td></tr>
                  <tr><td><strong>Check-out</strong></td><td>%s</td></tr>
                  <tr><td><strong>Number of guests</strong></td><td>%d</td></tr>
                  <tr><td><strong>Total price</strong></td><td>$%s</td></tr>
                </table>
                %s
                """.formatted(
                        escape(reservation.getGuestName()),
                        escape(campsite.getName()),
                        reservation.getCheckIn().format(DATE_FORMAT),
                        reservation.getCheckOut().format(DATE_FORMAT),
                        reservation.getNumberOfGuests(),
                        reservation.getTotalPrice(),
                        contactLine
                );
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : s.charAt(0) + s.substring(1).toLowerCase();
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
