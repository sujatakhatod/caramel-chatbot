package com.caramel.chat.web;

/** Raised when a session exceeds its request limit. Mapped to HTTP 429. */
public class RateLimitExceededException extends RuntimeException {

    public static final String USER_MESSAGE = "You're sending messages too quickly. Please wait a moment.";

    public RateLimitExceededException() {
        super(USER_MESSAGE);
    }
}
