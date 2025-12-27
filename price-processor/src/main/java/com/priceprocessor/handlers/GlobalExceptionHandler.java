package com.priceprocessor.handlers;

import com.priceprocessor.dtos.errors.ApiErrorResponse;
import com.priceprocessor.dtos.errors.ErrorCode;
import com.priceprocessor.exceptions.ProductNotFoundException;
import com.priceprocessor.exceptions.ProductNotFoundInStoreException;
import com.priceprocessor.exceptions.UserAlreadyExistsException;
import com.priceprocessor.exceptions.InvalidCredentialsException;
import com.priceprocessor.exceptions.crawler.InvalidStoreUrlException;
import com.priceprocessor.exceptions.crawler.ScraperException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUserAlreadyExists(
            UserAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        log.warn("Registration attempt failed: email already exists. Error: {} | URL: {}", ex.getMessage(), request.getRequestURI());

        ApiErrorResponse error = new ApiErrorResponse(
                ex.getMessage(),
                ErrorCode.AUTH_USER_ALREADY_EXISTS,
                HttpStatus.CONFLICT.value(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            Exception ex,
            HttpServletRequest request
    ) {
        log.warn("Authentication failed from IP: {} | URL: {}", request.getRemoteAddr(), request.getRequestURI());

        ApiErrorResponse error = new ApiErrorResponse(
                ex.getMessage(),
                ErrorCode.AUTH_INVALID_CREDENTIALS,
                HttpStatus.UNAUTHORIZED.value(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String detailedMessage = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    return fieldName + ": " + errorMessage;
                })
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", detailedMessage);

        ApiErrorResponse error = new ApiErrorResponse(
                "Validation failed: " + detailedMessage,
                ErrorCode.VALIDATION_FAILED,
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleProductNotFound(ProductNotFoundException ex, HttpServletRequest request) {
        log.info("Resource not found: {} | URL: {}", ex.getMessage(), request.getRequestURI());

        ApiErrorResponse error = new ApiErrorResponse(
                ex.getMessage(),
                ErrorCode.PRODUCT_NOT_FOUND,
                HttpStatus.NOT_FOUND.value(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ProductNotFoundInStoreException.class)
    public ResponseEntity<ApiErrorResponse> handleProductNotFoundInStore(ProductNotFoundInStoreException ex, HttpServletRequest request) {
        log.warn("External product search failed: {} | URL: {}", ex.getMessage(), request.getRequestURI());

        ApiErrorResponse error = new ApiErrorResponse(
                ex.getMessage(),
                ErrorCode.PRODUCT_NOT_IN_STORE,
                HttpStatus.NOT_FOUND.value(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected internal server error at URL: {}", request.getRequestURI(), ex);

        ApiErrorResponse error = new ApiErrorResponse(
                ex.getMessage(),
                ErrorCode.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InvalidStoreUrlException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidUrl(InvalidStoreUrlException ex) {
        ApiErrorResponse error = new ApiErrorResponse(
                ex.getMessage(),
                ErrorCode.VALIDATION_FAILED,
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ScraperException.class)
    public ResponseEntity<ApiErrorResponse> handleScraperGeneral(ScraperException ex) {
        ApiErrorResponse error = new ApiErrorResponse(
                ex.getMessage(),
                ErrorCode.INTERNAL_SERVER_ERROR,
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(UsernameNotFoundException ex) {
        ApiErrorResponse error = new ApiErrorResponse(
                ex.getMessage(),
                ErrorCode.USER_CREDENTIALS_INVALID,
                HttpStatus.FORBIDDEN.value(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }
}