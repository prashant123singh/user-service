package com.vinsguru.netflux.user.exceptions;

public class UnknownUserException extends RuntimeException {

    public UnknownUserException(Long userId) {
        super("Unknown user: " + userId);
    }
}
