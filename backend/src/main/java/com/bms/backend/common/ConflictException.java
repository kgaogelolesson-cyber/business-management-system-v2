package com.bms.backend.common;

/** The request is fine, but the current state of the data does not allow it (for example deleting a customer who has orders). Becomes HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
