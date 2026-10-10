package com.bms.backend.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * One row of the "customers" table.
 *
 * An "entity" is a Java class that Hibernate (inside Spring Data JPA) maps to a table. Spring Boot's
 * default naming rule turns Java names into column names: firstName -> first_name.
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // the database creates the id
    private Long customerId;

    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Hibernate needs a no-argument constructor. */
    protected Customer() {
    }

    /** A new customer: active, created now. */
    public Customer(String firstName, String lastName, String email, String phone, String address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    /** Changes the editable fields. Because the entity is "managed", Hibernate saves the change by itself. */
    public void change(String firstName, String lastName, String email, String phone, String address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }

    /** "Delete" = hide. The row stays, so history is never lost (same soft delete as Version 1). */
    public void deactivate() {
        this.active = false;
    }

    public Long getCustomerId() { return customerId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
