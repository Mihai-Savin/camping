package com.camp.reservations.web.ui;

import com.camp.reservations.domain.Reservation;
import com.camp.reservations.domain.ReservationStatus;
import com.camp.reservations.exception.InvalidReservationException;
import com.camp.reservations.exception.ReservationConflictException;
import com.camp.reservations.service.ReservationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The public "Find My Reservations" lookup has no guest login system - anyone
 * can type in an email. To avoid leaking one guest's booking details (or letting
 * them cancel another guest's reservation) to whoever else knows/guesses that
 * email, a session only "unlocks" full details for an email once this browser
 * has proven it belongs to that guest by booking with it in this same session.
 * Looking up someone else's email from a fresh session only ever returns a count.
 */
@Controller
@RequiredArgsConstructor
public class ReservationWebController {

    private static final String VERIFIED_EMAILS_SESSION_KEY = "verifiedGuestEmails";

    private final ReservationService reservationService;

    @GetMapping("/reservations")
    public String list(@RequestParam(required = false) String email, HttpSession session, Model model) {
        List<Reservation> reservations = List.of();
        long activeCount = 0;
        boolean verified = false;

        if (StringUtils.hasText(email)) {
            List<Reservation> found = reservationService.findByGuestEmail(email);
            activeCount = found.stream().filter(r -> r.getStatus() == ReservationStatus.CONFIRMED).count();
            verified = isVerifiedEmail(session, email);
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
                          BindingResult bindingResult, HttpSession session, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.reservationForm", bindingResult);
            redirectAttributes.addFlashAttribute("reservationForm", form);
            return "redirect:/campsites/" + form.getCampsiteId();
        }
        try {
            var reservation = reservationService.create(form.toRequest());
            markEmailVerified(session, reservation.getGuestEmail());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Reservation confirmed for " + reservation.getCampsite().getName()
                            + " from " + reservation.getCheckIn() + " to " + reservation.getCheckOut());
            return "redirect:/reservations?email=" + reservation.getGuestEmail();
        } catch (InvalidReservationException | ReservationConflictException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("reservationForm", form);
            return "redirect:/campsites/" + form.getCampsiteId();
        }
    }

    @PostMapping("/reservations/{id}/cancel")
    public String cancel(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        var existing = reservationService.findById(id);
        if (!isVerifiedEmail(session, existing.getGuestEmail())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Look up your reservations with your booking email before cancelling.");
            return "redirect:/reservations";
        }
        var reservation = reservationService.cancel(id);
        redirectAttributes.addFlashAttribute("successMessage", "Reservation #" + reservation.getId() + " cancelled");
        return "redirect:/reservations?email=" + reservation.getGuestEmail();
    }

    @SuppressWarnings("unchecked")
    private boolean isVerifiedEmail(HttpSession session, String email) {
        Set<String> verified = (Set<String>) session.getAttribute(VERIFIED_EMAILS_SESSION_KEY);
        return verified != null && verified.contains(email.toLowerCase());
    }

    @SuppressWarnings("unchecked")
    private void markEmailVerified(HttpSession session, String email) {
        Set<String> verified = (Set<String>) session.getAttribute(VERIFIED_EMAILS_SESSION_KEY);
        if (verified == null) {
            verified = new HashSet<>();
            session.setAttribute(VERIFIED_EMAILS_SESSION_KEY, verified);
        }
        verified.add(email.toLowerCase());
    }
}
