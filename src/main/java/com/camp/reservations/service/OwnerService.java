package com.camp.reservations.service;

import com.camp.reservations.domain.Owner;
import com.camp.reservations.exception.InvalidRegistrationException;
import com.camp.reservations.repository.OwnerRepository;
import com.camp.reservations.web.ui.RegisterForm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OwnerService {

    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Owner register(RegisterForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new InvalidRegistrationException("Passwords do not match");
        }
        if (ownerRepository.existsByEmailIgnoreCase(form.getEmail())) {
            throw new InvalidRegistrationException("An account with that email already exists");
        }
        Owner owner = Owner.builder()
                .email(form.getEmail())
                .password(passwordEncoder.encode(form.getPassword()))
                .displayName(form.getDisplayName())
                .build();
        return ownerRepository.save(owner);
    }
}
