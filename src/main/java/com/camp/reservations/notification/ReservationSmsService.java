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
 *
 * <p>While on a Twilio trial account, custom message bodies are rejected for
 * some destinations (error 572006) - only a fixed set of predefined template
 * names may be sent as-is, with no way to customize their wording. TWILIO_TRIAL_MODE
 * switches to those instead of the real reservation details; the full custom
 * message (with actual dates/names) only goes out once the Twilio account is
 * upgraded out of trial, at which point this flag should be removed.
 */
@Slf4j
@Component
public class ReservationSmsService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d");
    private static final String TRIAL_OWNER_TEMPLATE = "sms_account_alerts";
    private static final String TRIAL_GUEST_TEMPLATE = "sms_order_confirmation";

    private final RestClient twilioClient;
    private final String accountSid;
    private final String authToken;
    private final String apiKeySid;
    private final String apiKeySecret;
    private final String fromNumber;
    private final boolean trialMode;

    public ReservationSmsService(@Value("${twilio.account-sid:}") String accountSid,
                                  @Value("${twilio.auth-token:}") String authToken,
                                  @Value("${twilio.api-key-sid:}") String apiKeySid,
                                  @Value("${twilio.api-key-secret:}") String apiKeySecret,
                                  @Value("${twilio.from-number:}") String fromNumber,
                                  @Value("${twilio.trial-mode:false}") boolean trialMode) {
        this.twilioClient = RestClient.builder().baseUrl("https://api.twilio.com/2010-04-01").build();
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.apiKeySid = apiKeySid;
        this.apiKeySecret = apiKeySecret;
        this.fromNumber = fromNumber;
        this.trialMode = trialMode;
    }

    public void sendReservationNotifications(Reservation reservation) {
        boolean hasCredentials = StringUtils.hasText(accountSid) && StringUtils.hasText(fromNumber)
                && (hasApiKeyCredentials() || StringUtils.hasText(authToken));
        if (!hasCredentials) {
            log.info("Twilio credentials not configured; skipping SMS notifications for reservation {}",
                    reservation.getId());
            return;
        }

        Campsite campsite = reservation.getCampsite();
        String campsitePhone = campsite.getPhone();
        if (StringUtils.hasText(campsitePhone)) {
            String body = trialMode ? TRIAL_OWNER_TEMPLATE : ownerMessage(reservation, campsite);
            send(campsitePhone, body, reservation.getId(), "campsite");
        }

        String guestPhone = reservation.getGuestPhone();
        if (StringUtils.hasText(guestPhone)) {
            String body = trialMode ? TRIAL_GUEST_TEMPLATE : guestMessage(reservation, campsite);
            send(guestPhone, body, reservation.getId(), "guest");
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

    private boolean hasApiKeyCredentials() {
        return StringUtils.hasText(apiKeySid) && StringUtils.hasText(apiKeySecret);
    }

    private String basicAuth() {
        String credentials = hasApiKeyCredentials()
                ? apiKeySid + ":" + apiKeySecret
                : accountSid + ":" + authToken;
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
