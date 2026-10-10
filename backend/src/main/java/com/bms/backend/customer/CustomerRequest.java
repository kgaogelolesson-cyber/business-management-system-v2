package com.bms.backend.customer;

/** The JSON a browser sends to create or change a customer. */
public record CustomerRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        String address) {
}
