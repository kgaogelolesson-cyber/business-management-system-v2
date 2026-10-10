package com.bms.backend.common;

/** The thing asked for does not exist (or was deleted). Becomes HTTP 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
