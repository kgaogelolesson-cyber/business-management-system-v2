package com.bms.backend.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * One row of the "users" table: a login. It is called UserAccount (not User) so it is not
 * confused with Spring Security's own User class.
 *
 * The password is never stored as text. password_hash holds a BCrypt hash: a one-way
 * scrambled version that cannot be turned back into the password.
 */
@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String username;
    private String passwordHash;
    private String role;          // "ADMIN" or "EMPLOYEE"
    private Long employeeId;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    protected UserAccount() {
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public Long getEmployeeId() { return employeeId; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
