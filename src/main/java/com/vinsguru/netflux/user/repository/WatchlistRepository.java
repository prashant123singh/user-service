package com.vinsguru.netflux.user.repository;

import com.vinsguru.netflux.user.entity.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {

    @Query("select w.movieId from WatchlistItem w where w.user.id = :userId order by w.id")
    List<Long> findMovieIdsByUserId(Long userId);

    boolean existsByUserIdAndMovieId(Long userId, Long movieId);

    long deleteByUserIdAndMovieId(Long userId, Long movieId);
}
