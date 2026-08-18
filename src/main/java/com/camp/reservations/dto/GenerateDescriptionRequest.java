package com.camp.reservations.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerateDescriptionRequest(
        @NotBlank String name
) {
}
