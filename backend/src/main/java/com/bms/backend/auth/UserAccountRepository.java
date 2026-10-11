package com.bms.backend.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    /** "Admin" and "admin" are the same user, as in Version 1. */
    Optional<UserAccount> findByUsernameIgnoreCase(String username);
}
