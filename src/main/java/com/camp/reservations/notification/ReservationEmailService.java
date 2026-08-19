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
 * Standalone notification component: emails the campsite owner whenever a new
 * reservation is made. Deliberately isolated from the core booking logic in
 * ReservationService - a Resend outage or missing API key must never block or
 * roll back a reservation, so every failure here is caught and logged, never thrown.
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
            log.info("RESEND_API_KEY not configured; skipping owner notification email for reservation {}",
                    reservation.getId());
            return;
        }

        Campsite campsite = reservation.getCampsite();
        String ownerEmail = campsite.getOwner().getEmail();

        try {
            resendClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(Map.of(
                            "from", fromEmail,
                            "to", List.of(ownerEmail),
                            "subject", "New reservation: " + campsite.getName(),
                            "html", buildHtml(reservation, campsite)
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent reservation notification email to {} for reservation {}", ownerEmail, reservation.getId());
        } catch (RestClientException ex) {
            log.warn("Failed to send reservation notification email for reservation {}", reservation.getId(), ex);
        }
    }

    private String buildHtml(Reservation reservation, Campsite campsite) {
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

    private String capitalize(String s) {
        return s.isEmpty() ? s : s.charAt(0) + s.substring(1).toLowerCase();
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
