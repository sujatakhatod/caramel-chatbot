package com.caramel.chat.web;

/**
 * User-safe error body used for every rejected request. Never contains stack traces or provider payloads.
 *
 * @param error message suitable for display to the user
 */
public record ErrorResponse(String error) {
}
