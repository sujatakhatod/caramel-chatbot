---
name: spring-ai-chatbot
description: Build Java chatbots with Spring AI 2.0.x on Spring Boot. Use when implementing AI chat endpoints, calling or streaming an LLM through ChatModel / StreamingChatModel / ChatClient, building Prompts from System/User/Assistant messages, configuring LLM providers (OpenAI, Anthropic, Ollama, Amazon Bedrock, Mistral AI, Google GenAI) and ChatOptions, or handling streaming (SSE, Flux) responses in Java.
---

# Spring AI Chatbot (v2.0.2)

Based on the Spring AI Chat Model API reference: https://docs.spring.io/spring-ai/reference/api/chatmodel.html

The reference page documents the interfaces, not starters or properties. Provider starter and property names come from each provider's page (`/api/chat/<provider>-chat.html`). Check them there before pinning a name. Do not guess.

## 1. Core API

```java
public interface ChatModel extends Model<Prompt, ChatResponse>, StreamingChatModel {
    default String call(String message) {...}
    ChatResponse call(Prompt prompt);
}

public interface StreamingChatModel extends StreamingModel<Prompt, ChatResponse> {
    default Flux<String> stream(String message) {...}
    Flux<ChatResponse> stream(Prompt prompt);
}
```

- `call(String)` is a convenience. Real apps usually use `call(Prompt)` because it returns the full `ChatResponse`.
- `ChatModel` also extends `StreamingChatModel`, so one injected bean can do both.

### Synchronous call and reading the output

```java
ChatResponse response = chatModel.call(prompt);
Generation generation = response.getResult();            // first Generation
String text = generation.getOutput().getText();          // output is an AssistantMessage
ChatResponseMetadata meta = response.getMetadata();      // usage, model, etc.
```

- `ChatResponse` holds `List<Generation>` (`getResults()`) plus `ChatResponseMetadata`.
- `Generation.getOutput()` returns an `AssistantMessage`. `Generation.getMetadata()` returns `ChatGenerationMetadata` (finish reason, etc.).
- Use `getText()`. The older `getContent()` is gone.
- Guard against null or empty results (`response.getResult()` can be null) before dereferencing.

### Streaming

```java
Flux<String> tokens = chatModel.stream("Why is my request rate limited?");
Flux<ChatResponse> chunks = chatModel.stream(prompt);

// Extract text safely from chunks; some chunks carry only metadata
Flux<String> text = chunks
    .mapNotNull(r -> r.getResult() == null ? null : r.getResult().getOutput().getText());
```

Expose it over SSE. Return the `Flux` directly and never `.block()` or `.collectList()` it, which defeats streaming:

```java
@RestController
@RequestMapping("/api")
class ChatController {
    private final ChatService chat;
    ChatController(ChatService chat) { this.chat = chat; }

    @PostMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<String> chat(@RequestBody @Valid ChatRequest req) {
        return chat.stream(req.sessionId(), req.message())
                   .onErrorResume(e -> Flux.just("Sorry, something went wrong. Please try again."));
    }
}
record ChatRequest(@NotBlank String sessionId, @NotBlank @Size(max = 4000) String message) {}
```

Map provider timeouts and outages to a friendly message. Never leak stack traces to the client, and log the error class without the user's message content.

## 2. Prompt and Message roles

`Prompt` holds a `List<Message>` plus optional `ChatOptions`. The `MessageType` of each message is its conversational role.

```java
Prompt prompt = new Prompt(List.of(
    new SystemMessage("You are a support assistant for Acme's API. Say so when you don't know."),
    new UserMessage("Which models are available?"),
    new AssistantMessage("We currently offer ..."),
    new UserMessage("And the rate limits?")));
```

- `SystemMessage`: persona and rules. `UserMessage`: user input. `AssistantMessage`: prior model replies, which you replay to keep conversation state.
- `ChatModel` is stateless. For multi-turn chat, resend the history on each call or use `ChatClient` with a chat memory advisor keyed by session/conversation ID.
- Models without distinct roles treat input as `UserMessage`.
- Treat the system prompt as behavior-defining code. Keep it in a resource or config, and review changes like code changes.

## 3. ChatClient (higher-level, preferred for apps)

`ChatClient` wraps `ChatModel` with a fluent API, advisors (memory, RAG, logging) and structured output.

```java
@Service
class ChatService {
    private final ChatClient client;

    ChatService(ChatClient.Builder builder, ChatMemory chatMemory) {
        this.client = builder
            .defaultSystem("You are a support assistant ...")
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
            .build();
    }

    Flux<String> stream(String sessionId, String message) {
        return client.prompt()
            .user(message)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
            .stream()
            .content();
    }

    String ask(String message) {
        return client.prompt().user(message).call().content();
    }
}
```

Advisor and memory API names have shifted between releases. Confirm the exact builder and constant names (`MessageChatMemoryAdvisor`, `ChatMemory.CONVERSATION_ID`, `MessageWindowChatMemory`) against the 2.0.x ChatClient reference before relying on them.

## 4. Configuration and options

### Startup defaults (application.yml / .properties)

Defaults are applied when the `ChatModel` bean is created. Property prefixes are per provider:

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: ${CHAT_MODEL:gpt-4o-mini}
          temperature: 0.3
          max-tokens: 1024
```

Put the model name in config, never hardcoded in Java.

### Portable `ChatOptions`

`model`, `frequencyPenalty`, `maxTokens`, `presencePenalty`, `stopSequences`, `temperature`, `topK`, `topP`, plus `mutate()` to copy into a builder. Providers add their own (OpenAI: `logitBias`, `seed`, `user`, and others) through their provider-specific options classes.

### Runtime overrides: two different semantics

| Path | Behavior |
|---|---|
| `ChatModel.call(prompt)` / `stream(prompt)` with `ChatOptions` in the `Prompt` | The prompt's options **fully override** startup options. Supply a complete set. Unset fields do not fall back to the defaults. Pass `null` options to use the startup defaults. |
| `ChatClient` `.options(...)` / default options customizers | **Incremental delta**. Only the fields you set override the defaults. The rest are kept. |

```java
// ChatModel: full override, so set everything you care about
Prompt p = new Prompt(messages, ChatOptions.builder()
        .model("gpt-4o").temperature(0.0).maxTokens(512).build());

// ChatClient: delta. Only temperature changes, and model and the other defaults stay
client.prompt().user(q).options(ChatOptions.builder().temperature(0.0)).call().content();
```

If an override seems to "lose" a startup setting, you are on the `ChatModel` path. Either restate it or switch to `ChatClient`. Check whether `ChatClient.options(...)` takes a built `ChatOptions` or a builder in your 2.0.x version.

## 5. Multi-provider portability

Write business logic against `ChatModel`, `StreamingChatModel` and `ChatClient`, never against provider classes. Swap providers by changing the starter dependency and properties:

| Provider | Starter artifact (verify on provider page) | Property prefix |
|---|---|---|
| OpenAI | `spring-ai-starter-model-openai` | `spring.ai.openai` |
| Anthropic | `spring-ai-starter-model-anthropic` | `spring.ai.anthropic` |
| Ollama | `spring-ai-starter-model-ollama` | `spring.ai.ollama` |
| Amazon Bedrock Converse | `spring-ai-starter-model-bedrock-converse` | `spring.ai.bedrock.converse` |
| Mistral AI | `spring-ai-starter-model-mistral-ai` | `spring.ai.mistralai` |
| Google GenAI | `spring-ai-starter-model-google-genai` | `spring.ai.google.genai` |

Rules:
- Manage versions with the `spring-ai-bom` (`org.springframework.ai:spring-ai-bom:2.0.2`) in `dependencyManagement`. Do not pin individual Spring AI artifacts.
- Keep only one chat starter on the classpath, or select explicitly. With several, injecting a bare `ChatModel` is ambiguous. Use `@Qualifier` or the `spring.ai.model.chat` property.
- Stick to portable `ChatOptions` in shared code. Confine provider-specific options classes to one config class.
- Capabilities (streaming, multimodality, function calling) vary by provider, so check the provider page. Bedrock, for example, lists none on the Chat Model page.
- Anthropic has moved to the official Java SDK. Read the Anthropic migration guide before reusing old property names.
- Check the Upgrade Notes page when moving between Spring AI versions.

## 6. Best practices and guardrails

**Dependency injection**
- Use constructor injection with `final` fields. Inject `ChatClient.Builder` (autoconfigured) and build one `ChatClient` per use case. Inject `ChatModel` only for low-level access.
- Don't construct provider models by hand with `new OpenAiChatModel(...)` in app code. Let Boot autoconfiguration do it, so tests and provider swaps stay simple.
- Put the LLM call behind your own service interface so it can be mocked. In tests, mock `ChatModel` and return `new ChatResponse(List.of(new Generation(new AssistantMessage("hi"))))`. Never hit the real provider in unit tests.

**Secrets**
- Read API keys from environment variables via `${PROVIDER_API_KEY}` placeholders. Never put literals in `application.yml`, source or tests.
- For local dev, use a gitignored `.env` plus a committed `.env.example` with placeholders. Load it with `spring.config.import: optional:file:.env[.properties]`. Check that this works with your `.env` format.
- Never log keys, `Authorization` headers or raw user messages. Log request IDs, latency and error types.
- Keep keys server-side only. Never send them to the browser.

**Operations**
- Validate and cap input length. Rate-limit per client or session. Restrict CORS to the frontend origin.
- Set provider connect and read timeouts, and handle them with a user-friendly error.
- Stream with `Flux`. Don't buffer the whole answer. Don't `.block()` on a reactive (Netty) thread.
- Tell the model in the system prompt not to invent facts (pricing, SLAs, availability) and to say when it doesn't know.

**Avoid deprecated or stale patterns**
- Old starter names such as `spring-ai-openai-spring-boot-starter`. Use `spring-ai-starter-model-*`.
- `AssistantMessage.getContent()` and `Message.getContent()`. Use `getText()`.
- Hand-rolled history lists where a chat memory advisor fits.
- Mutating a shared `Prompt` or options object. Build new ones, or use `mutate()`.
- Milestone or snapshot repositories and 1.x snippets copied from old blog posts. Check every snippet against the 2.0.x docs.
- Stale API names from memory. When unsure about a signature, check the current Javadoc or reference page and don't invent methods.

## 7. Checklist before finishing

1. BOM at 2.0.2, one chat starter, key from an environment variable.
2. Model and options in `application.yml`.
3. Controller returns `Flux` with `text/event-stream` and has an error fallback.
4. Memory is keyed by session ID, and the system prompt lives in config or a resource.
5. Input limit, rate limit and CORS are in place. No user content is logged.
6. Tests mock `ChatModel` or the service, and the project builds.
