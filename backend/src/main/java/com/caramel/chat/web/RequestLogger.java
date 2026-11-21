package com.caramel.chat.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logs request metadata only: session ID, latency, and outcome (constitution IV, spec FR-016).
 * Never logs the user's message text, the reply text, or any credential.
 */
@Component
public class RequestLogger {

    private static final Logger log = LoggerFactory.getLogger("chat.request");

    public void log(String sessionId, long latencyMs, String outcome) {
        log.info("chat request sessionId={} latencyMs={} outcome={}", sessionId, latencyMs, outcome);
    }
}
