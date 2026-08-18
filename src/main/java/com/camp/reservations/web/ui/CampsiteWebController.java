package com.camp.reservations.web.ui;

import com.camp.reservations.domain.Campsite;
import com.camp.reservations.domain.ReservationStatus;
import com.camp.reservations.dto.GenerateDescriptionRequest;
import com.camp.reservations.dto.GenerateDescriptionResponse;
import com.camp.reservations.exception.DescriptionGenerationException;
import com.camp.reservations.exception.InvalidReservationException;
import com.camp.reservations.security.OwnerPrincipal;
import com.camp.reservations.service.CampsiteService;
import com.camp.reservations.service.DescriptionGenerationService;
import com.camp.reservations.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CampsiteWebController {

    private final CampsiteService campsiteService;
    private final ReservationService reservationService;
    private final DescriptionGenerationService descriptionGenerationService;

    @GetMapping("/campsites/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal OwnerPrincipal principal, Model model) {
        var campsite = campsiteService.findById(id);
        var upcoming = reservationService.findByCampsite(id).stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                .toList();

        var form = new ReservationForm();
        form.setCampsiteId(id);

        model.addAttribute("campsite", campsite);
        model.addAttribute("upcomingReservations", upcoming);
        model.addAttribute("isOwner", principal != null && campsite.getOwner().getId().equals(principal.getOwnerId()));
        if (!model.containsAttribute("reservationForm")) {
            model.addAttribute("reservationForm", form);
        }
        return "campsite-detail";
    }

    @GetMapping("/campsites/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("campsiteForm")) {
            model.addAttribute("campsiteForm", new CampsiteForm());
        }
        return "campsite-form";
    }

    @PostMapping("/campsites/generate-description")
    @ResponseBody
    public ResponseEntity<?> generateDescription(@Valid @RequestBody GenerateDescriptionRequest request) {
        try {
            String description = descriptionGenerationService.generateDescription(request.name());
            return ResponseEntity.ok(new GenerateDescriptionResponse(description));
        } catch (DescriptionGenerationException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/campsites")
    public String create(@Valid @ModelAttribute("campsiteForm") CampsiteForm form,
                          BindingResult bindingResult, @AuthenticationPrincipal OwnerPrincipal principal,
                          RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            return "campsite-form";
        }
        try {
            var campsite = campsiteService.create(form.toRequest(), principal.getOwner());
            redirectAttributes.addFlashAttribute("successMessage", "Campsite '" + campsite.getName() + "' created");
            return "redirect:/campsites/" + campsite.getId();
        } catch (InvalidReservationException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            return "campsite-form";
        }
    }

    @GetMapping("/campsites/{id}/edit")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal OwnerPrincipal principal, Model model) {
        Campsite campsite = requireOwnedCampsite(id, principal);

        var form = new CampsiteForm();
        form.setName(campsite.getName());
        form.setDescription(campsite.getDescription());
        form.setCapacity(campsite.getCapacity());
        form.setPricePerNight(campsite.getPricePerNight());
        form.setAmenities(campsite.getAmenities());
        form.setActive(campsite.isActive());

        model.addAttribute("campsiteForm", form);
        model.addAttribute("campsiteId", id);
        return "campsite-form";
    }

    @PostMapping("/campsites/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("campsiteForm") CampsiteForm form,
                          BindingResult bindingResult, @AuthenticationPrincipal OwnerPrincipal principal,
                          RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("campsiteId", id);
            return "campsite-form";
        }
        try {
            var campsite = campsiteService.update(id, form.toRequest(), principal.getOwner());
            redirectAttributes.addFlashAttribute("successMessage", "Campsite '" + campsite.getName() + "' updated");
            return "redirect:/campsites/" + campsite.getId();
        } catch (InvalidReservationException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            model.addAttribute("campsiteId", id);
            return "campsite-form";
        }
    }

    @PostMapping("/campsites/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal OwnerPrincipal principal,
                          RedirectAttributes redirectAttributes) {
        campsiteService.delete(id, principal.getOwner());
        redirectAttributes.addFlashAttribute("successMessage", "Campsite deleted");
        return "redirect:/my/campsites";
    }

    private Campsite requireOwnedCampsite(Long id, OwnerPrincipal principal) {
        Campsite campsite = campsiteService.findById(id);
        if (principal == null || !campsite.getOwner().getId().equals(principal.getOwnerId())) {
            throw new AccessDeniedException("You do not own campsite '" + campsite.getName() + "'");
        }
        return campsite;
    }
}
