package com.vinsguru.netflux.user.client;

import com.vinsguru.netflux.user.exceptions.MovieNotFoundException;
import com.vinsguru.netflux.user.exceptions.MovieServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class MovieClient {

    private final RestClient restClient;

    public MovieClient(RestClient movieRestClient) {
        this.restClient = movieRestClient;
    }

    public void verifyExists(Long movieId) {
        try {
            restClient.get()
                    .uri("/api/movies/{id}", movieId)
                    .retrieve()
                    .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
                        throw new MovieNotFoundException(movieId);
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new MovieServiceException();
                    })
                    .toBodilessEntity();
        } catch (ResourceAccessException ex) {
            throw new MovieServiceException(ex);
        }
    }
}
