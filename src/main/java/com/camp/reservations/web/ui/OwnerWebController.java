package com.camp.reservations.web.ui;

import com.camp.reservations.security.OwnerPrincipal;
import com.camp.reservations.service.CampsiteService;
import com.camp.reservations.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class OwnerWebController {

    private final CampsiteService campsiteService;
    private final ReservationService reservationService;

    @GetMapping("/my/campsites")
    public String myCampsites(@AuthenticationPrincipal OwnerPrincipal principal, Model model) {
        model.addAttribute("campsites", campsiteService.findByOwner(principal.getOwner()));
        return "my-campsites";
    }

    @GetMapping("/my/reservations")
    public String myReservations(@AuthenticationPrincipal OwnerPrincipal principal, Model model) {
        model.addAttribute("reservations", reservationService.findByOwner(principal.getOwner()));
        return "my-reservations";
    }

    @PostMapping("/my/reservations/{id}/cancel")
    public String cancelReservation(@PathVariable Long id, @AuthenticationPrincipal OwnerPrincipal principal,
                                     RedirectAttributes redirectAttributes) {
        var reservation = reservationService.cancelAsOwner(id, principal.getOwner());
        redirectAttributes.addFlashAttribute("successMessage", "Reservation #" + reservation.getId() + " cancelled");
        return "redirect:/my/reservations";
    }
}
