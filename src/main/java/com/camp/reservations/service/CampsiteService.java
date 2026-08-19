package com.camp.reservations.service;

import com.camp.reservations.domain.Campsite;
import com.camp.reservations.domain.FacilityType;
import com.camp.reservations.domain.Owner;
import com.camp.reservations.dto.CampsiteRequest;
import com.camp.reservations.exception.InvalidReservationException;
import com.camp.reservations.exception.ResourceNotFoundException;
import com.camp.reservations.repository.CampsiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampsiteService {

    private final CampsiteRepository campsiteRepository;

    public List<Campsite> findAllActive() {
        return campsiteRepository.findByActiveTrue();
    }

    public List<Campsite> findAll() {
        return campsiteRepository.findAll();
    }

    public List<Campsite> findByOwner(Owner owner) {
        return campsiteRepository.findByOwnerIdOrderByNameAsc(owner.getId());
    }

    public Campsite findById(Long id) {
        return campsiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campsite not found: " + id));
    }

    @Transactional
    public Campsite create(CampsiteRequest request, Owner owner) {
        if (campsiteRepository.existsByOwnerIdAndNameIgnoreCase(owner.getId(), request.name())) {
            throw new InvalidReservationException(
                    "You already have a campsite named '" + request.name() + "'");
        }
        Campsite campsite = Campsite.builder()
                .name(request.name())
                .description(request.description())
                .capacity(request.capacity())
                .pricePerNight(request.pricePerNight())
                .amenities(request.amenities())
                .imageUrl(request.imageUrl())
                .phone(request.phone())
                .facilityType(request.facilityType() != null ? request.facilityType() : FacilityType.OTHER)
                .active(request.active() == null || request.active())
                .owner(owner)
                .build();
        return campsiteRepository.save(campsite);
    }

    @Transactional
    public Campsite update(Long id, CampsiteRequest request, Owner currentOwner) {
        Campsite campsite = findById(id);
        requireOwnership(campsite, currentOwner);
        if (campsiteRepository.existsByOwnerIdAndNameIgnoreCaseAndIdNot(currentOwner.getId(), request.name(), id)) {
            throw new InvalidReservationException(
                    "You already have a campsite named '" + request.name() + "'");
        }
        campsite.setName(request.name());
        campsite.setDescription(request.description());
        campsite.setCapacity(request.capacity());
        campsite.setPricePerNight(request.pricePerNight());
        campsite.setAmenities(request.amenities());
        campsite.setImageUrl(request.imageUrl());
        campsite.setPhone(request.phone());
        campsite.setFacilityType(request.facilityType() != null ? request.facilityType() : FacilityType.OTHER);
        campsite.setActive(request.active() == null || request.active());
        return campsite;
    }

    @Transactional
    public void delete(Long id, Owner currentOwner) {
        Campsite campsite = findById(id);
        requireOwnership(campsite, currentOwner);
        campsiteRepository.delete(campsite);
    }

    private void requireOwnership(Campsite campsite, Owner currentOwner) {
        if (!campsite.getOwner().getId().equals(currentOwner.getId())) {
            throw new AccessDeniedException("You do not own campsite '" + campsite.getName() + "'");
        }
    }
}
