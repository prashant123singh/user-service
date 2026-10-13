package com.vinsguru.netflux.user.exceptions;

public class WatchlistItemNotFoundException extends RuntimeException {

    public WatchlistItemNotFoundException(Long movieId) {
        super("Movie not in watchlist: " + movieId);
    }
}
