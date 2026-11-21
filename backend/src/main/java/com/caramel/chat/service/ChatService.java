package com.caramel.chat.service;

import com.caramel.chat.config.ChatProperties;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Streams the assistant reply for one user message, using per-session memory.
 * Never buffers the full reply (constitution III).
 */
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ChatProperties props;

    public ChatService(ChatClient chatClient, ChatProperties props) {
        this.chatClient = chatClient;
        this.props = props;
    }

    /**
     * @param sessionId client-generated session ID, used as the conversation ID
     * @param message   user text, already validated by the caller
     * @return reply text fragments as they arrive; errors are mapped to {@link ProviderUnavailableException}
     */
    public Flux<String> stream(String sessionId, String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .content()
                .filter(Objects::nonNull)
                .filter(fragment -> !fragment.isEmpty())
                // Fails the reply if the provider goes silent for longer than the idle timeout.
                .timeout(props.providerIdleTimeout())
                .onErrorMap(ex -> new ProviderUnavailableException(ex));
    }
}
