package com.vinsguru.netflux.user.exceptions;

public class MovieNotFoundException extends RuntimeException {

    public MovieNotFoundException(Long movieId) {
        super("Movie not found: " + movieId);
    }
}
