package com.camp.reservations.notification;

import com.camp.reservations.domain.Campsite;
import com.camp.reservations.domain.Reservation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * Standalone notification component (sibling to ReservationEmailService):
 * texts both the campsite's own contact number and the guest via Twilio when
 * a reservation is made. The campsite's phone, not the owner account's, since
 * one owner can run several campsites with different on-site contact numbers.
 * Same isolation principle as email - a Twilio outage, missing credentials, or
 * a recipient with no phone on file must never block or fail a reservation.
 */
@Slf4j
@Component
public class ReservationSmsService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d");

    private final RestClient twilioClient;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    public ReservationSmsService(@Value("${twilio.account-sid:}") String accountSid,
                                  @Value("${twilio.auth-token:}") String authToken,
                                  @Value("${twilio.from-number:}") String fromNumber) {
        this.twilioClient = RestClient.builder().baseUrl("https://api.twilio.com/2010-04-01").build();
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
    }

    public void sendReservationNotifications(Reservation reservation) {
        if (!StringUtils.hasText(accountSid) || !StringUtils.hasText(authToken) || !StringUtils.hasText(fromNumber)) {
            log.info("Twilio credentials not configured; skipping SMS notifications for reservation {}",
                    reservation.getId());
            return;
        }

        Campsite campsite = reservation.getCampsite();
        String campsitePhone = campsite.getPhone();
        if (StringUtils.hasText(campsitePhone)) {
            send(campsitePhone, ownerMessage(reservation, campsite), reservation.getId(), "campsite");
        }

        String guestPhone = reservation.getGuestPhone();
        if (StringUtils.hasText(guestPhone)) {
            send(guestPhone, guestMessage(reservation, campsite), reservation.getId(), "guest");
        }
    }

    private void send(String to, String body, Long reservationId, String recipientLabel) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("To", to);
            form.add("From", fromNumber);
            form.add("Body", body);

            twilioClient.post()
                    .uri("/Accounts/{sid}/Messages.json", accountSid)
                    .header("Authorization", "Basic " + basicAuth())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent {} SMS notification for reservation {}", recipientLabel, reservationId);
        } catch (RestClientException ex) {
            log.warn("Failed to send {} SMS notification for reservation {}", recipientLabel, reservationId, ex);
        }
    }

    private String basicAuth() {
        String credentials = accountSid + ":" + authToken;
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private String ownerMessage(Reservation reservation, Campsite campsite) {
        return "New reservation for %s: %s guests, %s-%s. Guest: %s.".formatted(
                campsite.getName(),
                reservation.getNumberOfGuests(),
                reservation.getCheckIn().format(DATE_FORMAT),
                reservation.getCheckOut().format(DATE_FORMAT),
                reservation.getGuestName());
    }

    private String guestMessage(Reservation reservation, Campsite campsite) {
        return "Your reservation at %s (%s-%s) is confirmed. Total: $%s.".formatted(
                campsite.getName(),
                reservation.getCheckIn().format(DATE_FORMAT),
                reservation.getCheckOut().format(DATE_FORMAT),
                reservation.getTotalPrice());
    }
}
