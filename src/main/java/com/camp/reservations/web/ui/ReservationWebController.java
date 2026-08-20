package com.camp.reservations.web.ui;

import com.camp.reservations.domain.Reservation;
import com.camp.reservations.domain.ReservationStatus;
import com.camp.reservations.exception.InvalidReservationException;
import com.camp.reservations.exception.ReservationConflictException;
import com.camp.reservations.notification.NotificationFailure;
import com.camp.reservations.notification.NotificationOutcome;
import com.camp.reservations.security.OwnerPrincipal;
import com.camp.reservations.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The public "Find My Reservations" lookup has no dedicated guest-account
 * system - anyone can type in any email. Rather than trust that alone, full
 * details (and the ability to cancel) only unlock when the visitor is logged
 * in with an account whose own email matches the one being looked up. There
 * is no guest-only login; guests reuse the existing account system (the same
 * "Log In" / "Sign Up" used by campsite owners) - registering with the email
 * they booked with is enough to prove it's theirs. Just having booked, even
 * moments ago in the same browser, is never enough on its own.
 */
@Controller
@RequiredArgsConstructor
public class ReservationWebController {

    private final ReservationService reservationService;

    @GetMapping("/reservations")
    public String list(@RequestParam(required = false) String email,
                        @AuthenticationPrincipal OwnerPrincipal principal, Model model) {
        List<Reservation> reservations = List.of();
        long activeCount = 0;
        boolean verified = false;

        if (StringUtils.hasText(email)) {
            List<Reservation> found = reservationService.findByGuestEmail(email);
            activeCount = found.stream().filter(r -> r.getStatus() == ReservationStatus.CONFIRMED).count();
            verified = isOwnAccount(principal, email);
            if (verified) {
                reservations = found;
            }
        }

        model.addAttribute("reservations", reservations);
        model.addAttribute("email", email);
        model.addAttribute("verified", verified);
        model.addAttribute("activeCount", activeCount);
        return "reservations";
    }

    @PostMapping("/reservations")
    public String create(@Valid @ModelAttribute("reservationForm") ReservationForm form,
                          BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.reservationForm", bindingResult);
            redirectAttributes.addFlashAttribute("reservationForm", form);
            return "redirect:/campsites/" + form.getCampsiteId();
        }
        try {
            var result = reservationService.create(form.toRequest());
            var reservation = result.reservation();
            redirectAttributes.addFlashAttribute("successMessage",
                    "Reservation confirmed for " + reservation.getCampsite().getName()
                            + " from " + reservation.getCheckIn() + " to " + reservation.getCheckOut()
                            + ". Log in or sign up with " + reservation.getGuestEmail()
                            + " any time to view or manage it.");
            if (!result.notificationOutcome().allSucceeded()) {
                redirectAttributes.addFlashAttribute("warningMessage",
                        buildNotificationWarning(result.notificationOutcome()));
            }
            return "redirect:/reservations?email=" + reservation.getGuestEmail();
        } catch (InvalidReservationException | ReservationConflictException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("reservationForm", form);
            return "redirect:/campsites/" + form.getCampsiteId();
        }
    }

    private String buildNotificationWarning(NotificationOutcome outcome) {
        List<NotificationFailure> failures = outcome.failures();
        if (failures.size() == 1 && failures.get(0).channel() == null) {
            return "Your reservation is confirmed, but we couldn't reach the notification service, "
                    + "so no confirmation email or SMS could be sent. Please note your booking details above.";
        }

        Set<String> details = new LinkedHashSet<>();
        for (NotificationFailure failure : failures) {
            details.add(describeFailure(failure));
        }
        return "Your reservation is confirmed, but the following notifications could not be sent: "
                + String.join(", ", details) + ". Please note your booking details above.";
    }

    private String describeFailure(NotificationFailure failure) {
        String channel = switch (failure.channel() == null ? "" : failure.channel().toLowerCase()) {
            case "email" -> "Email";
            case "sms" -> "SMS";
            default -> "Notification";
        };
        String role = switch (failure.recipientRole() == null ? "" : failure.recipientRole()) {
            case "guest" -> " to you";
            case "owner" -> " to the campsite owner";
            default -> "";
        };
        return channel + role;
    }

    @PostMapping("/reservations/{id}/cancel")
    public String cancel(@PathVariable Long id, @AuthenticationPrincipal OwnerPrincipal principal,
                          RedirectAttributes redirectAttributes) {
        var existing = reservationService.findById(id);
        if (!isOwnAccount(principal, existing.getGuestEmail())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Log in with an account matching your booking email before cancelling.");
            return "redirect:/reservations";
        }
        var reservation = reservationService.cancel(id);
        redirectAttributes.addFlashAttribute("successMessage", "Reservation #" + reservation.getId() + " cancelled");
        return "redirect:/reservations?email=" + reservation.getGuestEmail();
    }

    private boolean isOwnAccount(OwnerPrincipal principal, String email) {
        return principal != null && principal.getOwner().getEmail().equalsIgnoreCase(email);
    }
}
