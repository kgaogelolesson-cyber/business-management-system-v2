package com.bms.backend.auth;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tells Spring Security where the users are. Spring Security does the rest of the login work:
 * it calls this class with the typed username, then compares the typed password with the
 * stored BCrypt hash. This replaces Version 1's AuthService.
 */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserAccountRepository users;

    public DatabaseUserDetailsService(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return User.withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .roles(account.getRole())               // "ADMIN" becomes the authority ROLE_ADMIN
                .disabled(!account.isActive())          // a deactivated user cannot log in
                .build();
    }
}
