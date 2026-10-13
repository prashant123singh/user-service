package com.vinsguru.netflux.user;

import com.vinsguru.netflux.user.dto.AddWatchlistRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@AutoConfigureRestTestClient
@AutoConfigureMockRestServiceServer
@Sql(scripts = {"/sql/insert-users.sql", "/sql/insert-watchlist.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/delete-all.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class WatchlistApiTest {

    private static final String USER_ID_HEADER = "X-User-Id";

    @Autowired
    RestTestClient restTestClient;

    @Autowired
    MockRestServiceServer mockServer;

    @Test
    @DisplayName("Should return movieIds in insertion order")
    void shouldReturnWatchlistInInsertionOrder() {
        var movieIds = getWatchlist(1001L);

        assertThat(movieIds).containsExactly(300L, 100L, 200L);
    }

    @Test
    @DisplayName("Should return an empty list when the user has no watchlist items")
    void shouldReturnEmptyWatchlist() {
        var movieIds = getWatchlist(1002L);

        assertThat(movieIds).isEmpty();
    }

    @Test
    @DisplayName("Should return 401 ProblemDetail when X-User-Id header is missing")
    void shouldReturn401WhenHeaderMissing() {
        var problem = restTestClient.get()
                .uri("/api/users/watchlist")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Should return 401 ProblemDetail when X-User-Id header is not numeric")
    void shouldReturn401WhenHeaderNotNumeric() {
        var problem = restTestClient.get()
                .uri("/api/users/watchlist")
                .header(USER_ID_HEADER, "abc")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Should verify the movie and add it to the watchlist")
    void shouldAddMovieToWatchlist() {
        mockServer.expect(requestTo(endsWith("/api/movies/400")))
                .andRespond(withSuccess("""
                        {"id": 400}
                        """, MediaType.APPLICATION_JSON));

        addToWatchlist(1001L, 400L)
                .expectStatus().isCreated();

        mockServer.verify();
        assertThat(getWatchlist(1001L)).containsExactly(300L, 100L, 200L, 400L);
    }

    @Test
    @DisplayName("Should return 201 without duplicating or calling movie-service when movie is already in watchlist")
    void shouldBeIdempotentWhenMovieAlreadyInWatchlist() {
        addToWatchlist(1001L, 100L)
                .expectStatus().isCreated();

        mockServer.verify();
        assertThat(getWatchlist(1001L)).containsExactly(300L, 100L, 200L);
    }

    @Test
    @DisplayName("Should return 404 ProblemDetail when movie-service does not know the movie")
    void shouldReturn404WhenMovieNotFound() {
        mockServer.expect(requestTo(endsWith("/api/movies/999")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        var problem = addToWatchlist(1001L, 999L)
                .expectStatus().isNotFound()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        mockServer.verify();
        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(getWatchlist(1001L)).containsExactly(300L, 100L, 200L);
    }

    @Test
    @DisplayName("Should return 502 ProblemDetail and leave watchlist unchanged when movie-service fails")
    void shouldReturn502WhenMovieServiceFails() {
        mockServer.expect(requestTo(endsWith("/api/movies/400")))
                .andRespond(withServerError());

        var problem = addToWatchlist(1001L, 400L)
                .expectStatus().isEqualTo(502)
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        mockServer.verify();
        assertThat(problem.getStatus()).isEqualTo(502);
        assertThat(getWatchlist(1001L)).containsExactly(300L, 100L, 200L);
    }

    @Test
    @DisplayName("Should return 400 ProblemDetail when movieId is missing")
    void shouldReturn400WhenMovieIdMissing() {
        var problem = addToWatchlist(1001L, null)
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should return 400 ProblemDetail when movieId is not positive")
    void shouldReturn400WhenMovieIdNotPositive() {
        var problem = addToWatchlist(1001L, 0L)
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should return 401 ProblemDetail when adding for an unknown user")
    void shouldReturn401WhenUserUnknown() {
        var problem = addToWatchlist(9999L, 400L)
                .expectStatus().isUnauthorized()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        mockServer.verify();
        assertThat(problem.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Should remove a movie from the watchlist")
    void shouldRemoveMovieFromWatchlist() {
        restTestClient.delete()
                .uri("/api/users/watchlist/{movieId}", 100)
                .header(USER_ID_HEADER, "1001")
                .exchange()
                .expectStatus().isNoContent();

        assertThat(getWatchlist(1001L)).containsExactly(300L, 200L);
    }

    @Test
    @DisplayName("Should return 404 ProblemDetail when removing a movie not in the watchlist")
    void shouldReturn404WhenRemovingMissingMovie() {
        var problem = restTestClient.delete()
                .uri("/api/users/watchlist/{movieId}", 999)
                .header(USER_ID_HEADER, "1001")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(404);
    }

    private List<Long> getWatchlist(Long userId) {
        return restTestClient.get()
                .uri("/api/users/watchlist")
                .header(USER_ID_HEADER, String.valueOf(userId))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Long>>() {})
                .returnResult()
                .getResponseBody();
    }

    private RestTestClient.ResponseSpec addToWatchlist(Long userId, Long movieId) {
        return restTestClient.post()
                .uri("/api/users/watchlist")
                .header(USER_ID_HEADER, String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AddWatchlistRequest(movieId))
                .exchange();
    }
}
