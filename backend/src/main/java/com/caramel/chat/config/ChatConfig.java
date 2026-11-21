package com.caramel.chat.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Wires per-session, in-process chat memory and the ChatClient with the persona system prompt.
 */
@Configuration
public class ChatConfig {

    @Bean
    ChatMemory chatMemory(ChatProperties props) {
        // In-process only: history is lost on restart and never persisted (spec FR-017).
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(props.memoryWindowSize())
                .build();
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder builder,
                          ChatMemory chatMemory,
                          @Value("classpath:prompts/system.md") Resource systemPrompt) {
        return builder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
