package com.example.domain.exception;

/**
 * Exception thrown when user lacks necessary permissions.
 */
public class ForbiddenAccessException extends DomainException {

    public ForbiddenAccessException(String message) {
        super(message);
    }

    public ForbiddenAccessException() {
        super("Forbidden: insufficient permissions for this operation");
    }
}
