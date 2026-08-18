package com.camp.reservations.web.ui;

import com.camp.reservations.service.CampsiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CampsiteService campsiteService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("campsites", campsiteService.findAllActive());
        return "home";
    }
}
