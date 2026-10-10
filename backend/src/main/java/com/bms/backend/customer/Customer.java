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
 * An "entity" is a Java class that Hibernate (the JPA implementation inside Spring Data JPA)
 * maps to a table. Spring Boot's default naming rule turns Java names into column names:
 * firstName -> first_name, customerId -> customer_id.
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

    // The database fills this in (DEFAULT now()), so Java never writes it.
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Hibernate needs a no-argument constructor. */
    protected Customer() {
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
