package com.bms.backend.common;

/** You are logged in, but this particular thing is not yours to see. Becomes HTTP 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
