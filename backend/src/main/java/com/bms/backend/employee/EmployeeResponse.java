package com.bms.backend.employee;

/**
 * What the API sends to the browser: the employee and their login (username and role).
 * The password hash is never sent. username and role are null if the employee has no login.
 */
public record EmployeeResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String position,
        String department,
        String hireDate,
        boolean active,
        String username,
        String role) {
}
