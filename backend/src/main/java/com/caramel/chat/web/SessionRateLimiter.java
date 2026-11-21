package com.caramel.chat.web;

import com.caramel.chat.config.ChatProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Per-session rolling one-minute request limit (spec FR-013, constitution IV).
 * In-process only, consistent with the MVP's in-memory design.
 */
@Component
public class SessionRateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final int limitPerWindow;
    private final Map<String, Deque<Instant>> requestTimes = new ConcurrentHashMap<>();

    public SessionRateLimiter(ChatProperties props) {
        this.limitPerWindow = props.rateLimitPerMinute();
    }

    /**
     * @return true if the request is allowed and recorded, false if the session is over its limit
     */
    public boolean tryAcquire(String sessionId) {
        Instant now = Instant.now();
        Instant windowStart = now.minus(WINDOW);
        Deque<Instant> times = requestTimes.computeIfAbsent(sessionId, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst().isBefore(windowStart)) {
                times.pollFirst();
            }
            if (times.size() >= limitPerWindow) {
                return false;
            }
            times.addLast(now);
            return true;
        }
    }
}
