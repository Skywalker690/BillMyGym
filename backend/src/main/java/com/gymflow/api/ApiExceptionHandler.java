package com.gymflow.api;

import com.gymflow.api.error.AppException;
import com.gymflow.api.response.ApiErrorResponse;
import com.gymflow.api.response.ApiFieldError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        logAppException(ex, request);
        return ResponseEntity.status(ex.getStatus())
            .body(ApiErrorResponse.of(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation failed for {} {} with {} field errors", request.getMethod(), request.getRequestURI(), ex.getErrorCount());
        List<ApiFieldError> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(this::toFieldError)
            .toList();
        return ResponseEntity.badRequest()
            .body(ApiErrorResponse.of("Validation failed", "VALIDATION_ERROR", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Constraint violation for {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        List<ApiFieldError> errors = ex.getConstraintViolations().stream()
            .map(violation -> new ApiFieldError(violation.getPropertyPath().toString(), violation.getMessage()))
            .toList();
        return ResponseEntity.badRequest()
            .body(ApiErrorResponse.of("Validation failed", "VALIDATION_ERROR", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed request body for {} {}", request.getMethod(), request.getRequestURI());
        return ResponseEntity.badRequest()
            .body(ApiErrorResponse.of("Malformed request body", "MALFORMED_REQUEST"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Bad request for {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest()
            .body(ApiErrorResponse.of(ex.getMessage(), "BAD_REQUEST"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Authentication failed for {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiErrorResponse.of(ex.getMessage(), "BAD_CREDENTIALS"));
    }

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(Exception ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiErrorResponse.of("Access is denied", "FORBIDDEN"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiErrorResponse.of("An unexpected error occurred", "INTERNAL_SERVER_ERROR"));
    }

    private ApiFieldError toFieldError(FieldError error) {
        return new ApiFieldError(error.getField(), error.getDefaultMessage());
    }

    private void logAppException(AppException ex, HttpServletRequest request) {
        String template = "Application error for {} {} status={} code={} message={}";
        if (ex.getStatus().is5xxServerError()) {
            log.error(
                template,
                request.getMethod(),
                request.getRequestURI(),
                ex.getStatus().value(),
                ex.getErrorCode(),
                ex.getMessage(),
                ex
            );
            return;
        }
        log.warn(
            template,
            request.getMethod(),
            request.getRequestURI(),
            ex.getStatus().value(),
            ex.getErrorCode(),
            ex.getMessage()
        );
    }
}
