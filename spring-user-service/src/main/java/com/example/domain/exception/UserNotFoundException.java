package com.example.domain.exception;

/**
 * Domain exception thrown when a user is not found.
 */
public class UserNotFoundException extends DomainException {

    public UserNotFoundException(String identifier) {
        super("User not found: " + identifier);
    }
}
