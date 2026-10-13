package com.bms.backend.employee;

/**
 * The JSON a browser sends to add or change an employee.
 * When ADDING: username, password and confirmPassword are used. When CHANGING they are ignored
 * (passwords change through the separate "reset password" call). role is "ADMIN" or "EMPLOYEE".
 * hireDate is text written yyyy-MM-dd, or empty.
 */
public record EmployeeRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        String position,
        String department,
        String hireDate,
        String username,
        String role,
        String password,
        String confirmPassword) {
}
