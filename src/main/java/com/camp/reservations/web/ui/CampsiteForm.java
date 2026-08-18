package com.camp.reservations.web.ui;

import com.camp.reservations.dto.CampsiteRequest;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CampsiteForm {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotNull
    @Min(1)
    private Integer capacity;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal pricePerNight;

    private String amenities;

    private boolean active = true;

    public CampsiteRequest toRequest() {
        return new CampsiteRequest(name, description, capacity, pricePerNight, amenities, active);
    }
}
