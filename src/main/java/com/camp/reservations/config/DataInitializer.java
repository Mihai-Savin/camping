package com.camp.reservations.config;

import com.camp.reservations.domain.Campsite;
import com.camp.reservations.domain.Owner;
import com.camp.reservations.repository.CampsiteRepository;
import com.camp.reservations.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CampsiteRepository campsiteRepository;
    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedTestOwner();

        if (campsiteRepository.count() > 0) {
            return;
        }

        Owner ranger = ownerRepository.save(Owner.builder()
                .email("ranger@pinewoodcamp.example")
                .password(passwordEncoder.encode("camping123"))
                .displayName("Ranger Sam")
                .build());

        campsiteRepository.saveAll(java.util.List.of(
                Campsite.builder()
                        .name("Whispering Pines")
                        .description("A shaded tent site tucked among tall pines, steps from the lake trail.")
                        .capacity(4)
                        .pricePerNight(new BigDecimal("29.97"))
                        .amenities("Fire pit, picnic table, bear box")
                        .active(true)
                        .owner(ranger)
                        .build(),
                Campsite.builder()
                        .name("Sunset Ridge")
                        .description("Open ridge-top site with panoramic sunset views, great for RVs.")
                        .capacity(6)
                        .pricePerNight(new BigDecimal("47.10"))
                        .amenities("Water hookup, electric hookup, fire pit")
                        .active(true)
                        .owner(ranger)
                        .build(),
                Campsite.builder()
                        .name("Creekside Hollow")
                        .description("Cozy spot right next to a babbling creek, popular with families.")
                        .capacity(5)
                        .pricePerNight(new BigDecimal("34.25"))
                        .amenities("Fire pit, picnic table, nearby restrooms")
                        .active(true)
                        .owner(ranger)
                        .build(),
                Campsite.builder()
                        .name("Eagle's Nest Cabin")
                        .description("A rustic one-room cabin with a covered porch overlooking the valley.")
                        .capacity(8)
                        .pricePerNight(new BigDecimal("77.07"))
                        .amenities("Beds, wood stove, covered porch")
                        .active(true)
                        .owner(ranger)
                        .build()
        ));
    }

    /**
     * A blank password isn't possible here — Owner.password is non-blank and login always
     * requires some input — so this uses a short, obviously-a-placeholder password instead.
     */
    private void seedTestOwner() {
        String testEmail = "savin.mihai@gmail.com";
        if (ownerRepository.existsByEmailIgnoreCase(testEmail)) {
            return;
        }
        ownerRepository.save(Owner.builder()
                .email(testEmail)
                .password(passwordEncoder.encode("test1234"))
                .displayName("Mihai (Test Account)")
                .build());
    }
}
