package de.uniks.vaadin.crm.security;

import de.uniks.vaadin.crm.security.model.CustomUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserVerificationService implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // TODO: retrieve login data for username from db and create CustomUserDetails to hand it to spring security handler
        CustomUserDetails customUserDetails = new CustomUserDetails(username, "1234");
        return customUserDetails;
    }
}
