package com.camp.reservations.config;

import com.camp.reservations.repository.OwnerRepository;
import com.camp.reservations.domain.Owner;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedTestOwner();
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
