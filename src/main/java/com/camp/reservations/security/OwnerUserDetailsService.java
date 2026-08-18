package com.camp.reservations.security;

import com.camp.reservations.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OwnerUserDetailsService implements UserDetailsService {

    private final OwnerRepository ownerRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return ownerRepository.findByEmailIgnoreCase(email)
                .map(OwnerPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("No account found for " + email));
    }
}
