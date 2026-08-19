package com.camp.reservations.dto;

import com.camp.reservations.domain.FacilityType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CampsiteRequest(
        @NotBlank String name,
        @NotBlank String description,
        @NotNull @Min(1) Integer capacity,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal pricePerNight,
        String amenities,
        String imageUrl,
        String phone,
        FacilityType facilityType,
        Boolean active
) {
}
