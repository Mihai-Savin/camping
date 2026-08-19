package com.camp.reservations.web.ui;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccountForm {

    @NotBlank
    private String displayName;

    private String phone;
}
