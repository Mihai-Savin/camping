package com.camp.reservations.service;

import com.camp.reservations.domain.Campsite;
import com.camp.reservations.domain.Owner;
import com.camp.reservations.dto.CampsiteRequest;
import com.camp.reservations.exception.InvalidReservationException;
import com.camp.reservations.repository.OwnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CampsiteServiceTest {

    @Autowired
    private CampsiteService campsiteService;

    @Autowired
    private OwnerRepository ownerRepository;

    private Owner alice;
    private Owner bob;

    @BeforeEach
    void setUp() {
        alice = ownerRepository.save(Owner.builder()
                .email("alice" + System.nanoTime() + "@example.com")
                .password("hashed")
                .displayName("Alice")
                .build());
        bob = ownerRepository.save(Owner.builder()
                .email("bob" + System.nanoTime() + "@example.com")
                .password("hashed")
                .displayName("Bob")
                .build());
    }

    private CampsiteRequest requestNamed(String name) {
        return new CampsiteRequest(name, "A nice spot", 4, new BigDecimal("30.00"), null, true);
    }

    @Test
    void createAssignsTheCreatingOwner() {
        Campsite campsite = campsiteService.create(requestNamed("Alice's Site " + System.nanoTime()), alice);

        assertThat(campsite.getOwner().getId()).isEqualTo(alice.getId());
    }

    @Test
    void ownerCanUpdateTheirOwnCampsite() {
        Campsite campsite = campsiteService.create(requestNamed("Alice's Site " + System.nanoTime()), alice);

        Campsite updated = campsiteService.update(campsite.getId(), requestNamed("Renamed Site"), alice);

        assertThat(updated.getName()).isEqualTo("Renamed Site");
    }

    @Test
    void nonOwnerCannotUpdateAnotherOwnersCampsite() {
        Campsite campsite = campsiteService.create(requestNamed("Alice's Site " + System.nanoTime()), alice);

        assertThatThrownBy(() -> campsiteService.update(campsite.getId(), requestNamed("Hijacked"), bob))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void nonOwnerCannotDeleteAnotherOwnersCampsite() {
        Campsite campsite = campsiteService.create(requestNamed("Alice's Site " + System.nanoTime()), alice);

        assertThatThrownBy(() -> campsiteService.delete(campsite.getId(), bob))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findByOwnerOnlyReturnsTheirCampsites() {
        campsiteService.create(requestNamed("Alice's Site " + System.nanoTime()), alice);
        campsiteService.create(requestNamed("Bob's Site " + System.nanoTime()), bob);

        assertThat(campsiteService.findByOwner(alice)).allMatch(c -> c.getOwner().getId().equals(alice.getId()));
    }

    @Test
    void ownerCannotCreateTwoCampsitesWithTheSameName() {
        campsiteService.create(requestNamed("Riverside Camp"), alice);

        assertThatThrownBy(() -> campsiteService.create(requestNamed("riverside camp"), alice))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("already have a campsite named");
    }

    @Test
    void ownerCannotRenameCampsiteToADuplicateOfTheirOwn() {
        campsiteService.create(requestNamed("Riverside Camp"), alice);
        Campsite other = campsiteService.create(requestNamed("Alice's Site " + System.nanoTime()), alice);

        assertThatThrownBy(() -> campsiteService.update(other.getId(), requestNamed("Riverside Camp"), alice))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("already have a campsite named");
    }

    @Test
    void ownerCanSaveACampsiteWithoutChangingItsOwnName() {
        Campsite campsite = campsiteService.create(requestNamed("Riverside Camp"), alice);

        Campsite updated = campsiteService.update(campsite.getId(), requestNamed("Riverside Camp"), alice);

        assertThat(updated.getName()).isEqualTo("Riverside Camp");
    }

    @Test
    void differentOwnersCanUseTheSameCampsiteName() {
        campsiteService.create(requestNamed("Riverside Camp"), alice);

        Campsite bobsCampsite = campsiteService.create(requestNamed("Riverside Camp"), bob);

        assertThat(bobsCampsite.getName()).isEqualTo("Riverside Camp");
    }
}
