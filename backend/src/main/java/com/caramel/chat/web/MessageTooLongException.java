package com.caramel.chat.web;

/** Raised when a message exceeds the configured maximum length. Mapped to HTTP 413. */
public class MessageTooLongException extends RuntimeException {

    public static final String USER_MESSAGE = "Messages must be 2,000 characters or fewer.";

    public MessageTooLongException() {
        super(USER_MESSAGE);
    }
}
