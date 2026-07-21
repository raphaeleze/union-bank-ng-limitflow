package com.limitflow.backend.presentation.exception;

import com.limitflow.backend.domain.exception.ForbiddenException;
import com.limitflow.backend.domain.exception.InvalidCredentialsException;
import com.limitflow.backend.domain.exception.NotFoundException;
import com.limitflow.backend.domain.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, ServerWebExchange exchange) {
        return respond(HttpStatus.NOT_FOUND, ex.getMessage(), exchange);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidation(ValidationException ex, ServerWebExchange exchange) {
        return respond(HttpStatus.BAD_REQUEST, ex.getMessage(), exchange);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex, ServerWebExchange exchange) {
        return respond(HttpStatus.FORBIDDEN, ex.getMessage(), exchange);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, ServerWebExchange exchange) {
        return respond(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", exchange);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex, ServerWebExchange exchange) {
        return respond(HttpStatus.UNAUTHORIZED, ex.getMessage(), exchange);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiError> handleBeanValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return respond(HttpStatus.BAD_REQUEST, message.isBlank() ? "Invalid request" : message, exchange);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, ServerWebExchange exchange) {
        log.error("Unhandled exception on {} {}", exchange.getRequest().getMethod(), exchange.getRequest().getPath(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.", exchange);
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, String message, ServerWebExchange exchange) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message,
                exchange.getRequest().getPath().value());
        return ResponseEntity.status(status).body(body);
    }
}
