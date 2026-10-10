package com.bms.backend.common;

/** The data sent by the user breaks a rule (for example "First name is required."). Becomes HTTP 400. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
