package com.camp.reservations.security;

import com.camp.reservations.domain.Owner;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class OwnerPrincipal implements UserDetails {

    private final Owner owner;

    public OwnerPrincipal(Owner owner) {
        this.owner = owner;
    }

    public Long getOwnerId() {
        return owner.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_OWNER"));
    }

    @Override
    public String getPassword() {
        return owner.getPassword();
    }

    @Override
    public String getUsername() {
        return owner.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
