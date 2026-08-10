package com.financemanager.exception;

/** Thrown for state conflicts, e.g. duplicate category names or registering an existing user. Maps to HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
