package de.uniks.vaadin.crm.security.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;

public class CustomUserDetails implements UserDetails {
    private String uuid;
    private String pin;
    private LocalDateTime expireTimestamp;
    private Boolean expired;

    public CustomUserDetails(String uuid, String pin) {
        this.expireTimestamp = LocalDateTime.now().plus(1, ChronoUnit.MINUTES);
        this.uuid = uuid;
        this.pin = pin;
        this.expired = false;
    }

    public void setExpired() {
        this.expired = true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return this.pin;
    }

    @Override
    public String getUsername() {
        return this.uuid;
    }

    @Override
    public boolean isAccountNonExpired() {
        return !expired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return LocalDateTime.now().isBefore(expireTimestamp);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
