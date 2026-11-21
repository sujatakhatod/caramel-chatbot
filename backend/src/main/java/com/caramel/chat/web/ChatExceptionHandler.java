package com.caramel.chat.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;

/**
 * Maps request-level failures to the contract's user-safe JSON body. Stack traces are never returned.
 */
@RestControllerAdvice
public class ChatExceptionHandler {

    static final String BLANK_MESSAGE = "Please enter a message.";
    static final String INVALID_REQUEST = "Please enter a message.";

    @ExceptionHandler(WebExchangeBindException.class)
    ResponseEntity<ErrorResponse> handleValidation(WebExchangeBindException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(BLANK_MESSAGE));
    }

    @ExceptionHandler(ServerWebInputException.class)
    ResponseEntity<ErrorResponse> handleBadInput(ServerWebInputException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(INVALID_REQUEST));
    }

    @ExceptionHandler(MessageTooLongException.class)
    ResponseEntity<ErrorResponse> handleTooLong(MessageTooLongException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse(MessageTooLongException.USER_MESSAGE));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ErrorResponse> handleRateLimited(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse(RateLimitExceededException.USER_MESSAGE));
    }
}
