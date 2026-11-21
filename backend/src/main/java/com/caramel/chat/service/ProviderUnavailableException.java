package com.caramel.chat.service;

/**
 * Raised when the model provider times out or fails. The message is always user-safe; the cause is
 * kept only for the server-side error class in logs.
 */
public class ProviderUnavailableException extends RuntimeException {

    public static final String USER_MESSAGE = "Sorry, something went wrong. Please try again.";

    public ProviderUnavailableException(Throwable cause) {
        super(USER_MESSAGE, cause);
    }
}
