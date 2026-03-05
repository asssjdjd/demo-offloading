package com.example.domain.exception;

/**
 * Exception thrown when a request is not authorized at the gateway level.
 */
public class UnauthorizedAccessException extends DomainException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public UnauthorizedAccessException() {
        super("Unauthorized: request did not pass gateway authentication");
    }
}
