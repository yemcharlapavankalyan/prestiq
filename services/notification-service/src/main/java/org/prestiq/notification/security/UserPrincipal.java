package org.prestiq.notification.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean isInternal;

    public UserPrincipal(
            UUID id,
            String email,
            Collection<? extends GrantedAuthority> authorities,
            boolean isInternal
    ) {
        this.id = id;
        this.email = email;
        this.authorities = authorities != null ? authorities : List.of();
        this.isInternal = isInternal;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public boolean isInternal() {
        return isInternal;
    }

    public boolean hasRole(String role) {
        String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals(target));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email != null ? email : (id != null ? id.toString() : "internal-service");
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
