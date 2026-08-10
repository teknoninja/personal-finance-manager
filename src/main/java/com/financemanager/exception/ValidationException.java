package com.financemanager.exception;

/** Thrown for semantic validation failures not caught by bean validation. Maps to HTTP 400. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
