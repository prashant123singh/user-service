package com.vinsguru.netflux.user.exceptions;

public class MovieServiceException extends RuntimeException {

    public MovieServiceException(Throwable cause) {
        super("Movie service is unavailable", cause);
    }

    public MovieServiceException() {
        super("Movie service is unavailable");
    }
}
