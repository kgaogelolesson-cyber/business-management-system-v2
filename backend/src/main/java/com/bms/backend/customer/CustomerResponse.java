package com.bms.backend.customer;

import java.time.LocalDateTime;

/**
 * What the API sends to the browser (a DTO, "data transfer object").
 * We send this instead of the Customer entity so the database design is not exposed
 * directly, and so we choose exactly which fields leave the server.
 * A Java "record" is a short way to write a class that only holds data.
 */
public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String address,
        LocalDateTime createdAt) {

    static CustomerResponse from(Customer c) {
        return new CustomerResponse(
                c.getCustomerId(),
                c.getFirstName(),
                c.getLastName(),
                c.getEmail(),
                c.getPhone(),
                c.getAddress(),
                c.getCreatedAt());
    }
}
