# PRD: Caramel AI Tech Support Chat Assistant

## 1. Objective

Build a customer service chat assistant for **Caramel AI**, an LLM inferencing company similar to OpenRouter. The assistant provides technical support on LLM inferencing: API usage, model availability, rate limits, latency, errors, billing, and integration help.

## 2. Tech Stack

| Layer | Choice |
|---|---|
| Backend | Java (Spring Boot) |
| AI framework | Spring AI |
| Frontend | React + TypeScript |
| LLM | `gemma-4-26b-a4b-it` |
| Service provider | Google via AI Studio |

## 3. Target Users

- Developers integrating Caramel AI's inference API.
- Technical and billing contacts at customer companies.

## 4. Scope

### In scope (MVP)
- Web chat UI with message history for the current session.
- Streaming responses from the LLM.
- System prompt that defines the assistant as Caramel AI tech support, limited to LLM inferencing topics.
- Multi-turn conversation memory (per session).
- Graceful handling of off-topic questions, errors, and LLM outages.

### Out of scope (MVP)
- User authentication and accounts.
- Live human handoff / ticketing integration.
- Persistent chat history across sessions.
- RAG over company docs (candidate for a later phase).

## 5. Functional Requirements

1. **Chat endpoint**: `POST /api/chat` accepts a session ID and user message, and streams the reply (SSE).
2. **Conversation memory**: use Spring AI chat memory, keyed by session ID.
3. **Prompting**: system prompt sets persona, tone (concise, friendly, technical), and boundaries. The assistant must not invent pricing, SLAs, or model availability, and should say when it does not know.
4. **Frontend**: chat window with message list, input box, send button, typing/streaming indicator, error state, and "New chat" button.
5. **Configuration**: the Google AI Studio API key is stored in a local `.env` file at the project root as `GOOGLE_API_KEY=<key>`. The backend reads it as an environment variable (Spring Boot can import `.env` via `spring.config.import`). The model name lives in `application.yml`. `.env` must be listed in `.gitignore`, and a `.env.example` with placeholder values should be committed instead.

## 6. Non-Functional Requirements

- **Latency**: first token displayed in under 3 seconds under normal conditions.
- **Security**: the Google API key is held server-side only, loaded from `.env`, and never sent to the browser or logged. `.env` is excluded from source control. CORS limited to the frontend origin. Basic input length limit and rate limiting.
- **Reliability**: user-friendly error message on provider timeouts or failures.
- **Observability**: log requests, latency, and errors, without logging sensitive user content.

## 7. Architecture

```
React (TypeScript)  --SSE/HTTP-->  Spring Boot + Spring AI  -->  Google AI Studio (gemma-4-26b-a4b-it)
```

- Spring AI `ChatClient` with a chat memory advisor.
- The model is reached through Spring AI's Google AI Studio integration (Gemini API / OpenAI-compatible endpoint, to be confirmed during implementation).

## 8. Milestones

1. Project scaffolding: Spring Boot backend and Vite React-TS frontend.
2. Backend chat endpoint with Spring AI and Gemma, non-streaming.
3. Streaming, plus session memory.
4. Chat UI wired to the backend.
5. System prompt tuning, error handling, and rate limiting.
6. Testing and documentation (README with run instructions, including how to create `.env` from `.env.example`).

## 9. Success Metrics

- Assistant answers on-topic LLM inferencing questions accurately in manual test scenarios.
- Off-topic requests are politely declined.
- End-to-end chat works with streaming and multi-turn context.

## 10. Open Questions

- Should the assistant be grounded in Caramel AI's real docs (RAG) in a later phase?
- Is the Spring AI integration for AI Studio best done through the Google GenAI starter or the OpenAI-compatible endpoint, given `gemma-4-26b-a4b-it` support?
- What escalation path should be shown when the assistant can't resolve an issue (email, ticket link)?
