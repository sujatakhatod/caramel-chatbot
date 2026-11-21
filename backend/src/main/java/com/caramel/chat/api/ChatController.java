package com.caramel.chat.api;

import com.caramel.chat.config.ChatProperties;
import com.caramel.chat.service.ChatService;
import com.caramel.chat.service.ProviderUnavailableException;
import com.caramel.chat.web.MessageTooLongException;
import com.caramel.chat.web.RateLimitExceededException;
import com.caramel.chat.web.RequestLogger;
import com.caramel.chat.web.SessionRateLimiter;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * POST /api/chat: streams the assistant reply as Server-Sent Events.
 * Event contract: zero or more {@code token}, then exactly one {@code done} or one {@code error}.
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final ChatProperties props;
    private final SessionRateLimiter rateLimiter;
    private final RequestLogger requestLogger;

    public ChatController(ChatService chatService,
                          ChatProperties props,
                          SessionRateLimiter rateLimiter,
                          RequestLogger requestLogger) {
        this.chatService = chatService;
        this.props = props;
        this.rateLimiter = rateLimiter;
        this.requestLogger = requestLogger;
    }

    @PostMapping(path = "/chat",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, String>>> chat(@Valid @RequestBody ChatRequest request) {
        // Pre-stream rejections throw here, so the advice returns a JSON error body, not a stream.
        if (request.message().length() > props.maxMessageLength()) {
            throw new MessageTooLongException();
        }
        if (!rateLimiter.tryAcquire(request.sessionId())) {
            throw new RateLimitExceededException();
        }

        String sessionId = request.sessionId();
        long startNanos = System.nanoTime();
        AtomicReference<String> outcome = new AtomicReference<>("ok");

        Flux<ServerSentEvent<Map<String, String>>> tokens = chatService
                .stream(sessionId, request.message())
                .map(fragment -> ServerSentEvent.<Map<String, String>>builder()
                        .event("token")
                        .data(Map.of("text", fragment))
                        .build());

        Mono<ServerSentEvent<Map<String, String>>> done = Mono.just(
                ServerSentEvent.<Map<String, String>>builder()
                        .event("done")
                        .data(Map.of())
                        .build());

        return tokens
                .concatWith(done)
                .doOnError(ex -> outcome.set(ex.getClass().getSimpleName()))
                .onErrorResume(ProviderUnavailableException.class, ex -> Mono.just(
                        ServerSentEvent.<Map<String, String>>builder()
                                .event("error")
                                .data(Map.of("message", ProviderUnavailableException.USER_MESSAGE))
                                .build()))
                .doFinally(signal -> requestLogger.log(
                        sessionId,
                        (System.nanoTime() - startNanos) / 1_000_000,
                        outcome.get()));
    }
}
