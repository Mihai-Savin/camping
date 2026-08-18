package com.camp.reservations.web.ui;

import com.camp.reservations.domain.Owner;
import com.camp.reservations.security.OwnerPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.camp.reservations.web.ui")
public class GlobalModelAttributes {

    @ModelAttribute("currentOwner")
    public Owner currentOwner(@AuthenticationPrincipal OwnerPrincipal principal) {
        return principal == null ? null : principal.getOwner();
    }
}
