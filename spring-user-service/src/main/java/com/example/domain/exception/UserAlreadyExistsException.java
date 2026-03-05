package com.example.domain.exception;

/**
 * Domain exception thrown when a duplicate user is detected.
 */
public class UserAlreadyExistsException extends DomainException {

    public UserAlreadyExistsException(String field, String value) {
        super("User already exists with " + field + ": " + value);
    }
}
