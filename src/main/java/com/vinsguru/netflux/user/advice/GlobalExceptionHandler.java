package com.vinsguru.netflux.user.advice;

import com.vinsguru.netflux.user.exceptions.InvalidCredentialsException;
import com.vinsguru.netflux.user.exceptions.MovieNotFoundException;
import com.vinsguru.netflux.user.exceptions.MovieServiceException;
import com.vinsguru.netflux.user.exceptions.UnknownUserException;
import com.vinsguru.netflux.user.exceptions.UsernameAlreadyExistsException;
import com.vinsguru.netflux.user.exceptions.WatchlistItemNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ProblemDetail handleUsernameAlreadyExists(UsernameAlreadyExistsException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    @ExceptionHandler(UnknownUserException.class)
    public ProblemDetail handleUnknownUser(UnknownUserException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingHeader(MissingRequestHeaderException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Missing header: " + ex.getHeaderName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        if (ex.getParameter().hasParameterAnnotation(RequestHeader.class)) {
            return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid header: " + ex.getName());
        }
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", "Invalid parameter: " + ex.getName());
    }

    @ExceptionHandler({WatchlistItemNotFoundException.class, MovieNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(MovieServiceException.class)
    public ProblemDetail handleMovieService(MovieServiceException ex) {
        log.error("Movie service call failed", ex);
        return problem(HttpStatus.BAD_GATEWAY, "Bad Gateway", ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class})
    public ProblemDetail handleValidation(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", "Validation failed");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", "Malformed or missing request body");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
