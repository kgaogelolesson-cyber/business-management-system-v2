package com.bms.backend.employee;

/** The JSON for "reset this employee's password". */
public record PasswordRequest(String newPassword, String confirmPassword) {
}
