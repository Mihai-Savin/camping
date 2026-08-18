package com.camp.reservations.web.ui;

import com.camp.reservations.exception.InvalidRegistrationException;
import com.camp.reservations.service.OwnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final OwnerService ownerService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                            BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        try {
            ownerService.register(form);
        } catch (InvalidRegistrationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "register";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Account created. You can log in now.");
        return "redirect:/login";
    }
}
