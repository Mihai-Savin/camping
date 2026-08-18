package com.camp.reservations.web.ui;

import com.camp.reservations.domain.Reservation;
import com.camp.reservations.exception.InvalidReservationException;
import com.camp.reservations.exception.ReservationConflictException;
import com.camp.reservations.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ReservationWebController {

    private final ReservationService reservationService;

    @GetMapping("/reservations")
    public String list(@RequestParam(required = false) String email, Model model) {
        var reservations = StringUtils.hasText(email)
                ? reservationService.findByGuestEmail(email)
                : List.<Reservation>of();
        model.addAttribute("reservations", reservations);
        model.addAttribute("email", email);
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
            var reservation = reservationService.create(form.toRequest());
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
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        var reservation = reservationService.cancel(id);
        redirectAttributes.addFlashAttribute("successMessage", "Reservation #" + reservation.getId() + " cancelled");
        return "redirect:/reservations";
    }
}
