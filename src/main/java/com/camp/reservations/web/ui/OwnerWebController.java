package com.camp.reservations.web.ui;

import com.camp.reservations.security.OwnerPrincipal;
import com.camp.reservations.service.CampsiteService;
import com.camp.reservations.service.OwnerService;
import com.camp.reservations.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class OwnerWebController {

    private final CampsiteService campsiteService;
    private final ReservationService reservationService;
    private final OwnerService ownerService;

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

    @GetMapping("/my/account")
    public String account(@AuthenticationPrincipal OwnerPrincipal principal, Model model) {
        if (!model.containsAttribute("accountForm")) {
            var form = new AccountForm();
            form.setDisplayName(principal.getOwner().getDisplayName());
            form.setPhone(principal.getOwner().getPhone());
            model.addAttribute("accountForm", form);
        }
        return "account";
    }

    @PostMapping("/my/account")
    public String updateAccount(@Valid @ModelAttribute("accountForm") AccountForm form, BindingResult bindingResult,
                                 @AuthenticationPrincipal OwnerPrincipal principal, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "account";
        }
        ownerService.updateProfile(principal.getOwner(), form);
        redirectAttributes.addFlashAttribute("successMessage", "Account updated");
        return "redirect:/my/account";
    }
}
