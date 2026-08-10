package com.financemanager.exception;

/** Thrown when a user attempts to access or modify a resource they do not own. Maps to HTTP 403. */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
