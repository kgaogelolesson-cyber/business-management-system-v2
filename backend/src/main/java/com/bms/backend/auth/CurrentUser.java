package com.bms.backend.auth;

/** Who is logged in, as sent to the browser. */
public record CurrentUser(String username, String role) {
}
