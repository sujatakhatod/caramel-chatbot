package com.caramel.chat.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Chat limits and settings, bound from the {@code chat.*} keys in application.yml.
 *
 * @param maxMessageLength    maximum user message length in characters (spec FR-012)
 * @param rateLimitPerMinute  maximum requests per session per minute (spec FR-013)
 * @param allowedOrigin       the only browser origin allowed to call the API (spec FR-014)
 * @param memoryWindowSize    maximum messages kept per session in chat memory
 * @param providerIdleTimeout maximum gap between reply tokens before the reply is failed
 */
@ConfigurationProperties(prefix = "chat")
public record ChatProperties(
        int maxMessageLength,
        int rateLimitPerMinute,
        String allowedOrigin,
        int memoryWindowSize,
        Duration providerIdleTimeout) {
}
