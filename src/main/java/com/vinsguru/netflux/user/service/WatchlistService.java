package com.vinsguru.netflux.user.service;

import com.vinsguru.netflux.user.client.MovieClient;
import com.vinsguru.netflux.user.entity.WatchlistItem;
import com.vinsguru.netflux.user.exceptions.UnknownUserException;
import com.vinsguru.netflux.user.exceptions.WatchlistItemNotFoundException;
import com.vinsguru.netflux.user.repository.UserRepository;
import com.vinsguru.netflux.user.repository.WatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;
    private final MovieClient movieClient;

    public WatchlistService(WatchlistRepository watchlistRepository, UserRepository userRepository, MovieClient movieClient) {
        this.watchlistRepository = watchlistRepository;
        this.userRepository = userRepository;
        this.movieClient = movieClient;
    }

    public List<Long> getMovieIds(Long userId) {
        return watchlistRepository.findMovieIdsByUserId(userId);
    }

    @Transactional
    public void add(Long userId, Long movieId) {
        if (!userRepository.existsById(userId)) {
            throw new UnknownUserException(userId);
        }
        if (watchlistRepository.existsByUserIdAndMovieId(userId, movieId)) {
            return;
        }
        movieClient.verifyExists(movieId);
        var item = new WatchlistItem();
        item.setUser(userRepository.getReferenceById(userId));
        item.setMovieId(movieId);
        watchlistRepository.save(item);
    }

    @Transactional
    public void remove(Long userId, Long movieId) {
        if (watchlistRepository.deleteByUserIdAndMovieId(userId, movieId) == 0) {
            throw new WatchlistItemNotFoundException(movieId);
        }
    }
}
