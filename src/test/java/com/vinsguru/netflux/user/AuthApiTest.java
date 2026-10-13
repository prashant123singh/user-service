package com.vinsguru.netflux.user;

import com.vinsguru.netflux.user.dto.LoginRequest;
import com.vinsguru.netflux.user.dto.LoginResponse;
import com.vinsguru.netflux.user.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@AutoConfigureRestTestClient
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts = "/sql/delete-all.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AuthApiTest {

    @Autowired
    RestTestClient restTestClient;

    @Autowired
    KeyPair testRsaKeyPair;

    @Test
    @DisplayName("Should register a new user who can then log in")
    void shouldRegisterUser() {
        restTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest("carol", "pa55word", "Carol"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody().isEmpty();

        var response = login(new LoginRequest("carol", "pa55word"))
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response.name()).isEqualTo("Carol");
        assertThat(response.token()).isNotBlank();
    }

    @Test
    @Sql(scripts = "/sql/insert-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @DisplayName("Should return 409 ProblemDetail when username already exists")
    void shouldReturn409WhenUsernameExists() {
        var problem = restTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest("alice", "another", "Another Alice"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(409);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRegisterRequests")
    @DisplayName("Should return 400 ProblemDetail when a register field is missing or blank")
    void shouldReturn400WhenRegisterFieldInvalid(RegisterRequest request) {
        var problem = restTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("Validation failed");
    }

    @ParameterizedTest(name = "body: {0}")
    @ValueSource(strings = {"", "{bad json"})
    @DisplayName("Should return 400 ProblemDetail when the register body is malformed or empty")
    void shouldReturn400WhenRegisterBodyMalformed(String body) {
        var problem = postRaw("/api/users/register", body)
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("Malformed or missing request body");
    }

    @Test
    @Sql(scripts = "/sql/insert-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @DisplayName("Should return an RS256 JWT with the expected claims on login")
    void shouldReturnJwtOnLogin() {
        var response = login(new LoginRequest("alice", "secret123"))
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();

        var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) testRsaKeyPair.getPublic()).build();
        var jwt = decoder.decode(response.token());

        assertThat(response.name()).isEqualTo("Alice");
        assertThat(jwt.getHeaders()).containsEntry("alg", "RS256");
        assertThat(jwt.getSubject()).isEqualTo("1001");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("user-service");
        assertThat(jwt.getClaimAsString("name")).isEqualTo("Alice");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
    }

    @Test
    @Sql(scripts = "/sql/insert-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @DisplayName("Should return 401 ProblemDetail when password is wrong")
    void shouldReturn401WhenPasswordWrong() {
        var problem = login(new LoginRequest("alice", "wrong"))
                .expectStatus().isUnauthorized()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(401);
        assertThat(problem.getDetail()).isEqualTo("Invalid username or password");
    }

    @Test
    @Sql(scripts = "/sql/insert-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @DisplayName("Should return the same 401 ProblemDetail when username is unknown")
    void shouldReturn401WhenUsernameUnknown() {
        var problem = login(new LoginRequest("nobody", "secret123"))
                .expectStatus().isUnauthorized()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(401);
        assertThat(problem.getDetail()).isEqualTo("Invalid username or password");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidLoginRequests")
    @DisplayName("Should return 400 ProblemDetail when a login field is missing or blank")
    void shouldReturn400WhenLoginFieldInvalid(LoginRequest request) {
        var problem = login(request)
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("Validation failed");
    }

    @ParameterizedTest(name = "body: {0}")
    @ValueSource(strings = {"", "{bad json"})
    @DisplayName("Should return 400 ProblemDetail when the login body is malformed or empty")
    void shouldReturn400WhenLoginBodyMalformed(String body) {
        var problem = postRaw("/api/users/login", body)
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("Malformed or missing request body");
    }

    static Stream<RegisterRequest> invalidRegisterRequests() {
        return Stream.of(
                new RegisterRequest(null, "pa55word", "Dave"),
                new RegisterRequest(" ", "pa55word", "Dave"),
                new RegisterRequest("dave", null, "Dave"),
                new RegisterRequest("dave", " ", "Dave"),
                new RegisterRequest("dave", "pa55word", null),
                new RegisterRequest("dave", "pa55word", " ")
        );
    }

    static Stream<LoginRequest> invalidLoginRequests() {
        return Stream.of(
                new LoginRequest(null, "secret123"),
                new LoginRequest(" ", "secret123"),
                new LoginRequest("alice", null),
                new LoginRequest("alice", " ")
        );
    }

    private RestTestClient.ResponseSpec login(LoginRequest request) {
        return restTestClient.post()
                .uri("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange();
    }

    private RestTestClient.ResponseSpec postRaw(String uri, String body) {
        return restTestClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange();
    }
}
