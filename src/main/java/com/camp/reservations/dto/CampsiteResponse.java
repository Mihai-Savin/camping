package com.camp.reservations.dto;

import com.camp.reservations.domain.Campsite;

import java.math.BigDecimal;

public record CampsiteResponse(
        Long id,
        String name,
        String description,
        Integer capacity,
        BigDecimal pricePerNight,
        String amenities,
        boolean active
) {
    public static CampsiteResponse from(Campsite c) {
        return new CampsiteResponse(
                c.getId(), c.getName(), c.getDescription(), c.getCapacity(),
                c.getPricePerNight(), c.getAmenities(), c.isActive()
        );
    }
}
