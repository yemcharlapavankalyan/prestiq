package org.prestiq.scholarship.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails, Principal {

    private final UUID id;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean internal;

    public UserPrincipal(UUID id, String email, Collection<? extends GrantedAuthority> authorities, boolean internal) {
        this.id = id;
        this.email = email;
        this.authorities = authorities != null ? authorities : Collections.emptyList();
        this.internal = internal;
    }

    public static UserPrincipal of(UUID id, String email, List<String> roleNames, boolean internal) {
        List<SimpleGrantedAuthority> auths = (roleNames != null)
                ? roleNames.stream().map(SimpleGrantedAuthority::new).toList()
                : Collections.emptyList();
        return new UserPrincipal(id, email, auths, internal);
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public boolean isInternal() {
        return internal;
    }

    public boolean hasRole(String role) {
        if (authorities == null) return false;
        String fullRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return authorities.stream().anyMatch(a -> a.getAuthority().equals(fullRole));
    }

    @Override
    public String getName() {
        return email != null ? email : (id != null ? id.toString() : "anonymous");
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email != null ? email : (id != null ? id.toString() : "anonymous");
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
