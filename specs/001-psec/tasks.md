# Tasks: Caramel AI Tech Support Chat (psec)

**Input**: Design documents from `/specs/001-psec/`

**Prerequisites**: [plan.md](plan.md) (required), [spec.md](spec.md) (required), [research.md](research.md),
[data-model.md](data-model.md), [contracts/chat-api.md](contracts/chat-api.md),
[quickstart.md](quickstart.md)

**Tests**: Not requested in the spec. No automated test tasks are generated. MVP acceptance uses the
manual scenarios in quickstart.md (constitution, Development Workflow).

**Organization**: Tasks are grouped by user story (spec.md, priority order) so each story can be
built and checked independently.

**Assumptions**:
- Backend base package: `com.caramel.chat`, under `backend/src/main/java/com/caramel/chat/`.
- Frontend source root: `frontend/src/`.
- Open item O1 (live confirmation of `gemma-4-26b-a4b-it`) is tracked as task T015. Code can be
  written before it, but no live provider call or acceptance test passes until T015 is resolved.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: User story label (US1 to US4). Used only in user story phases.
- Each description names the exact file path.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project skeletons and root files.

- [X] T001 Create the Spring Boot backend project in `backend/pom.xml`. Use a Spring Boot 4.1.x parent,
  `<java.version>17</java.version>`, and import the `spring-ai-bom` (`org.springframework.ai`,
  type `pom`, scope `import`). Confirm the BOM version is 2.1.0 against Spring Boot 4.1.x before
  pinning (research R2, open item O2).
- [X] T002 [P] Create the Vite React TypeScript project in `frontend/` (`frontend/package.json`,
  `frontend/tsconfig.json`, `frontend/vite.config.ts`, `frontend/src/main.tsx`).
- [X] T003 [P] Create `.gitignore` at the repository root. It MUST list `.env`, `frontend/node_modules/`,
  and `backend/target/`. The spec requires `.env` to be excluded from source control.
- [X] T004 [P] Create `.env.example` at the repository root containing only the placeholder line
  `GOOGLE_API_KEY=your-google-ai-studio-key-here`. Do not put a real key in this file.
- [X] T005 Add `spring-ai-starter-model-google-genai` to `backend/pom.xml` with no explicit version
  (managed by the BOM). Depends on T001.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Configuration, safety rails, and shared types that every user story needs.

**CRITICAL**: No user story work can be checked end to end until this phase is complete.

- [X] T006 Create `backend/src/main/resources/application.yml`. It MUST:
  - import the root `.env` as an optional file (`spring.config.import`, path relative to `backend/`);
  - set `spring.ai.google.genai.api-key: ${GOOGLE_API_KEY}`, with no literal key;
  - set `spring.ai.google.genai.chat.model: gemma-4-26b-a4b-it` (value gated by T015);
  - set `chat.max-message-length: 2000`, `chat.rate-limit-per-minute: 20`,
    `chat.allowed-origin: http://localhost:5173`, and `chat.memory-window-size` (a positive integer).
  Depends on T005. Property names must match the Google GenAI reference, not guesses.
- [X] T007 Create `backend/src/main/java/com/caramel/chat/config/ChatProperties.java` as a
  `@ConfigurationProperties(prefix = "chat")` record that binds the T006 values. Depends on T006.
- [X] T008 [P] Create `backend/src/main/resources/prompts/system.md`, the persona prompt. It MUST:
  - list the supported topics verbatim from constitution I: "API usage, model availability, rate limits,
    latency, errors, billing, and integration";
  - politely decline off-topic requests and point back to those topics (FR-008);
  - forbid inventing "pricing, SLAs, model availability, or rate-limit figures" (FR-009);
  - tell the assistant to say when it does not know;
  - set tone to "concise, friendly, and technical" (FR-010).
  Any later wording change is a behavior change and must be re-checked (constitution I).
- [X] T009 Create `backend/src/main/java/com/caramel/chat/config/ChatConfig.java`. Define a
  `MessageWindowChatMemory` bean sized by `chat.memory-window-size`, and a `ChatClient` bean with the
  system prompt from `prompts/system.md` and a message-chat-memory advisor. Confirm exact class and
  constant names against the pinned 2.1.0 reference (skill notes may be stale). Depends on T007, T008.
- [X] T010 Create `backend/src/main/java/com/caramel/chat/config/CorsConfig.java`. Allow only
  `chat.allowed-origin` for `/api/chat`. Reject any other origin with HTTP 403 and the JSON body
  `{"error": "Request not allowed."}` (contract, FR-014). Depends on T007.
- [X] T011 [P] Create `backend/src/main/java/com/caramel/chat/web/ErrorResponse.java`, a record with a
  single `String error` field. Every rejected-request body uses this shape (contract).
- [X] T012 [P] Create `backend/src/main/java/com/caramel/chat/web/RequestLogger.java`. It logs session
  ID, latency in milliseconds, outcome, and error class. It MUST NOT log message text, the reply
  text, or the API key (constitution IV, FR-016).
- [X] T013 [P] Create `frontend/src/services/sessionId.ts`. It generates the session ID with
  `crypto.randomUUID()` and keeps it in memory only. It is not written to localStorage, because
  history must not persist across sessions (FR-017). Depends on T002.
- [X] T014 [P] Create `frontend/src/types/chat.ts` from data-model.md:
  - `Message` with `role` (`USER` or `ASSISTANT`; `SYSTEM` is never shown to the user), `text`, and
    `createdAt`;
  - `ReplyStatus` with exactly `PENDING`, `STREAMING`, `COMPLETE`, `FAILED`.
  Depends on T002.
- [ ] T015 **GATE (manual, blocks live provider calls)**: Resolve open item O1. Run the one-request
  check in quickstart.md section 1 with a real key kept in the shell environment. Record the result in
  `specs/001-psec/research.md` under R1. If the model is rejected on AI Studio, try the Vertex AI route
  via the same starter (research R1). If all routes reject it, stop and escalate to the project owner.
  Do not substitute a model silently.

**Checkpoint**: Foundation ready. User story work can start. T015 must pass before live tests.

---

## Phase 3: User Story 1 - Ask a technical question and get a streamed answer (Priority: P1) MVP

**Goal**: A user sends one on-topic question and sees the reply stream in progressively.

**Independent Test**: Open a new chat, send one on-topic question, and confirm a streamed answer appears
with a pending indicator that clears on completion (quickstart scenario 1). Requires T015.

### Implementation for User Story 1

- [X] T016 [P] [US1] Create `backend/src/main/java/com/caramel/chat/api/ChatRequest.java`, a record with:
  - `sessionId`: `@NotBlank`. Data-model rule: "Generated by the browser client. Required on every
    request. Non-blank."
  - `message`: `@NotBlank`. Data-model rule: "at most 2,000 characters, non-blank."
  Do NOT put `@Size` on `message`. Over-length input must return HTTP 413 per the contract, and
  `@Size` would produce 400. The length check is handled in T031.
- [X] T017 [US1] Create `backend/src/main/java/com/caramel/chat/service/ChatService.java`.
  `stream(sessionId, message)` returns `Flux<String>` from `ChatClient` `.prompt().user(message)`, with
  the conversation ID set to `sessionId`, then `.stream().content()`. Use `mapNotNull` to drop empty
  chunks. Never call `block()` or `collectList()` (constitution III). Depends on T009.
- [X] T018 [US1] In `backend/src/main/java/com/caramel/chat/service/ChatService.java`, map provider
  failures with `onErrorResume` to a `ProviderUnavailableException` that carries only the friendly
  text "Sorry, something went wrong. Please try again." Log the error class only (T012). Depends on T017.
- [X] T019 [US1] Create `backend/src/main/java/com/caramel/chat/api/ChatController.java`. It handles
  `POST /api/chat`, consumes `application/json`, and produces `text/event-stream`. It returns
  `Flux<ServerSentEvent<Map<String, String>>>` with:
  - one `token` event per fragment, data `{"text": "..."}`;
  - exactly one `done` event, data `{}`, on normal completion;
  - one `error` event, data `{"message": "..."}`, on failure, replacing `done`.
  It also records each request through `RequestLogger` (T012). Matches `contracts/chat-api.md`.
  Depends on T016, T017, T018, T012.
- [X] T020 [P] [US1] Create `frontend/src/services/chatClient.ts`. It exports `streamChat(sessionId,
  message, handlers)`, which uses `fetch` with `POST` and reads the body via `ReadableStream`. It parses
  `token`, `done`, and `error` events. Do not use `EventSource`, which cannot send POST bodies. The
  client holds no API key. Depends on T002.
- [X] T021 [P] [US1] Create `frontend/src/components/MessageList.tsx`. It renders `Message[]` from
  T014, with user and assistant messages visually distinct.
- [X] T022 [P] [US1] Create `frontend/src/components/MessageInput.tsx` with a text box and a Send
  button. Disable both while the reply status is `PENDING` or `STREAMING`. Do not send empty or
  whitespace-only text (spec edge case).
- [X] T023 [P] [US1] Create `frontend/src/components/StreamingIndicator.tsx`. It is visible while the
  status is `PENDING` or `STREAMING` (FR-004).
- [X] T024 [US1] Create `frontend/src/pages/ChatPage.tsx`. It holds the message list, the session ID
  from T013, and the `ReplyStatus`. It appends `token` text as it arrives, sets `COMPLETE` on `done`,
  and sets `FAILED` on `error`, keeping any partial text. Depends on T013, T014, T020, T021, T022, T023.
- [X] T025 [US1] Create `frontend/src/App.tsx` to render `ChatPage`, and wire `frontend/src/main.tsx` to
  mount `App`. Depends on T024.

**Checkpoint**: MVP delivered. A user can ask one question and see a streamed reply.

---

## Phase 4: User Story 2 - Follow up within the same conversation (Priority: P2)

**Goal**: Follow-up questions are answered using earlier turns of the same session.

**Independent Test**: Ask a rate-limit question, then "and for the free tier?", and confirm the answer
uses the earlier context (quickstart scenario 2). Requires Phase 3 and T015.

- [ ] T026 [US2] Verify `backend/src/main/java/com/caramel/chat/config/ChatConfig.java` attaches the
  memory advisor on every call, and that `ChatService.java` passes the same `sessionId` as the
  conversation ID each time. If a follow-up loses context in scenario 2, fix the advisor wiring here.
  Depends on T009, T017.
- [X] T027 [US2] Update `frontend/src/pages/ChatPage.tsx` so one session ID is kept for the whole chat
  and sent with every message. Messages are not reset between turns. Depends on T024.
- [X] T028 [US2] Update `frontend/src/components/MessageList.tsx` so all earlier messages stay visible
  and the view scrolls to the newest message on each update (US2 acceptance scenario 2). Depends on T021.

**Checkpoint**: Multi-turn conversations work within a session.

---

## Phase 5: User Story 3 - Decline off-topic requests and recover from failures (Priority: P3)

**Goal**: Off-topic and unverifiable questions get safe answers. Limits and failures produce friendly
messages, and the user can keep going.

**Independent Test**: Run quickstart scenarios 3 to 5. Off-topic gets a polite decline, unverifiable
pricing gets "I don't know", and a simulated timeout gets a friendly message with a working retry.

- [X] T029 [P] [US3] Create `backend/src/main/java/com/caramel/chat/web/SessionRateLimiter.java`. It
  allows at most `chat.rate-limit-per-minute` requests per `sessionId` in a rolling one-minute window.
  It exposes `boolean tryAcquire(String sessionId)` (FR-013). Depends on T007.
- [X] T030 [US3] Create `backend/src/main/java/com/caramel/chat/web/ChatExceptionHandler.java`, a
  `@RestControllerAdvice` that maps pre-stream errors to the contract's JSON `ErrorResponse`:
  - blank input: 400, "Please enter a message."
  - over-length input: 413, "Messages must be 2,000 characters or fewer."
  - rate limit: 429, "You're sending messages too quickly. Please wait a moment."
  Define the exception types `MessageTooLongException` and `RateLimitExceededException` alongside it.
  Depends on T011.
- [X] T031 [US3] Update `backend/src/main/java/com/caramel/chat/api/ChatController.java`. Before
  streaming starts, throw `MessageTooLongException` when `message` length exceeds
  `chat.max-message-length`, and throw `RateLimitExceededException` when `SessionRateLimiter`
  (T029) refuses the request. Depends on T019, T029, T030.
- [X] T032 [US3] Configure provider connect and read timeouts in `backend/src/main/resources/application.yml`.
  Use the property names in the Google GenAI reference, confirmed at scaffolding, not guessed. A timeout
  must reach the T018 mapping and return the friendly error event. Depends on T006.
- [X] T033 [P] [US3] Create `frontend/src/components/ErrorBanner.tsx`. It shows a friendly message and
  a Retry action that resends the last user message. It shows no technical text. It is independent of
  the chat list so partial replies remain visible.
- [X] T034 [US3] Update `frontend/src/services/chatClient.ts` so non-200 JSON responses (400, 403, 413,
  429) are read and their `error` text is surfaced to the UI. Depends on T020.
- [X] T035 [US3] Update `frontend/src/components/MessageInput.tsx` to block sending messages over
  2,000 characters and show the limit text (FR-012). Depends on T022.
- [X] T036 [US3] Update `frontend/src/pages/ChatPage.tsx` to render `ErrorBanner` when the status is
  `FAILED` or a pre-stream error is returned, keeping partial text visible. Depends on T033, T024.

**Checkpoint**: Failure and limit handling work end to end.

---

## Phase 6: User Story 4 - Start a new chat (Priority: P3)

**Goal**: "New chat" clears the window and starts a fresh session with no earlier context.

**Independent Test**: Hold a multi-turn conversation, select "New chat", ask a follow-up, and confirm no
earlier content is referenced (quickstart scenario 6).

- [X] T037 [P] [US4] Create `frontend/src/components/NewChatButton.tsx`, which calls an `onNewChat`
  handler when clicked.
- [X] T038 [US4] Update `frontend/src/pages/ChatPage.tsx` so `onNewChat` generates a new session ID with
  `sessionId.ts`, clears the messages, and resets the status to idle. Render `NewChatButton`. The new
  session ID means the backend memory starts empty, so no backend change is needed. Depends on T037, T036.

**Checkpoint**: All four user stories are functional.

---

## Final Phase: Polish & Cross-Cutting Concerns

- [X] T039 [P] Create `README.md` at the repository root with run instructions, including how to create
  `.env` from `.env.example` (PRD milestone 6). Depends on T004.
- [X] T040 [P] Add the build, run, and test commands to `CLAUDE.md` once they exist (constitution,
  Development Workflow). Depends on T001, T002.
- [X] T041 [P] Secret and content exposure check: search the frontend source and build output for
  `GOOGLE_API_KEY` and the key value, and confirm backend logs for a full quickstart run contain no
  message text (quickstart section 5 and scenario 7; constitution II and IV).
- [X] T042 [P] CORS check: send a `POST /api/chat` with an unlisted `Origin` and confirm the 403 JSON
  response (quickstart security spot check; FR-014).
- [ ] T043 Run quickstart.md scenarios 1 to 7 and record the results in `specs/001-psec/quickstart.md`.
  Measure first-token latency and confirm SC-001 (at least 95% within 3 seconds). Requires T015.
- [ ] T044 Run the scripted off-topic and unverifiable-pricing prompts for SC-002 and SC-003, and record
  the results in `specs/001-psec/quickstart.md`. Requires T015.

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Setup. Blocks all user stories.
- **User stories (Phases 3 to 6)**: Start after Foundational. Live testing also needs T015.
  - US1 (P1) is the MVP and blocks US2, US3, and US4, because they extend `ChatPage.tsx` and
    `ChatController.java`.
  - US2 and US4 are small follow-ups to US1.
  - US3 backend work (T029, T030) can start during Phase 2 and merge after US1.
- **Polish**: After the user stories that it checks.

### Within each story

- Models and types before services. Services before controllers. Controllers before frontend wiring.
- Tasks that edit the same file (for example T017 and T018, or T024, T027, and T036) run in sequence.

### Parallel opportunities

- Phase 1: T002, T003, T004 together.
- Phase 2: T008, T011, T012, T013, T014 together.
- US1: T016, T020, T021, T022, T023 together. Then T024 and T025.
- US3: T029 and T033 together.
- Polish: T039, T040, T041, T042 together.

### Parallel example: User Story 1

```text
Run together:
  T016 ChatRequest.java
  T020 chatClient.ts
  T021 MessageList.tsx
  T022 MessageInput.tsx
  T023 StreamingIndicator.tsx
Then: T017 -> T018 -> T019 (backend, in order), and T024 -> T025 (frontend, in order).
```

---

## Implementation Strategy

### MVP first (User Story 1 only)

1. Phase 1 (T001 to T005).
2. Phase 2 (T006 to T015). Resolve T015 before live testing.
3. Phase 3 (T016 to T025).
4. Stop and validate with quickstart scenario 1. The MVP is a streamed single-turn chat.

### Incremental delivery

1. MVP (US1) as above.
2. Add US2 (multi-turn), then validate scenario 2.
3. Add US3 (limits, failures, off-topic), then validate scenarios 3 to 5.
4. Add US4 (New chat), then validate scenario 6.
5. Polish, then full quickstart run and SC checks (T043, T044).

---

## Summary

| Metric | Value |
|--------|-------|
| Total tasks | 44 |
| Phase 1 Setup | 5 (T001 to T005) |
| Phase 2 Foundational | 10 (T006 to T015), including gate T015 |
| US1 (P1, MVP) | 10 (T016 to T025) |
| US2 (P2) | 3 (T026 to T028) |
| US3 (P3) | 8 (T029 to T036) |
| US4 (P3) | 2 (T037 to T038) |
| Polish | 6 (T039 to T044) |
| Parallel tasks marked [P] | 20 |
| MVP scope | Phases 1 to 3 (T001 to T025) |

**Format validation**: Every task starts with `- [ ]`, has a sequential ID, uses `[P]` only for
parallelizable tasks, carries a `[USn]` label only in user story phases, and names a file path or
explicit artifact.

**Test coverage**: No automated tests were requested, so none are generated. Acceptance uses the
manual scenarios in quickstart.md.

---

## Implementation Log (2026-11-21)

**Completed**: 40 of 44 tasks. **Open**: T015, T026, T043, T044. All four need a real Google API key
and a live model call, which this environment does not have.

### Deviations from the task text (decided during implementation)

- **Port 8081, not 8080.** Port 8080 is in use on this machine by Jenkins, which is outside this
  project. The backend uses `server.port: 8081`, and the frontend default `VITE_API_BASE_URL` matches.
- **T032 (timeouts).** The Google GenAI starter has no connect or read timeout property (checked
  against `spring-ai-autoconfigure-model-google-genai` 2.1.0). Instead, `ChatService` applies a
  Reactor idle timeout set by `chat.provider-idle-timeout: 30s`. A silent provider fails the reply
  with the friendly error event.
- **Reactive stack.** The backend uses WebFlux (`spring-boot-starter-webflux`) so `ChatController`
  can return `Flux<ServerSentEvent>`.
- **Jackson 3.** Spring Boot 4 uses `tools.jackson.databind.ObjectMapper`, not the Jackson 2 class.
- **Bug in my first draft, caught in review and fixed before the build:** the first-token flag in `ChatPage.tsx` is decided
  outside the state updater, because StrictMode can run updaters twice.

### Verification performed (placeholder key, no provider call succeeded)

| Check | Result |
|-------|--------|
| Backend compiles (`mvn compile`) | Pass |
| Frontend type-check and build (`npm run build`, strict TS) | Pass |
| Backend starts with the placeholder key | Pass |
| Blank message returns 400 JSON | Pass |
| 2,001-character message returns 413 JSON | Pass |
| Foreign `Origin` returns 403 JSON | Pass |
| Allowed-origin preflight returns 200 with `Access-Control-Allow-Origin` | Pass |
| Valid message with a bad key returns a friendly SSE `error` event and no stack trace | Pass |
| 21st request in a minute returns 429 (20 allowed) | Pass |
| Logs contain no message text, no key, no stack traces | Pass |
| Frontend build output contains no key | Pass |

### Not verified (requires O1 / a real key)

- The `token` and `done` events from a successful reply, and therefore streaming latency (SC-001).
- Gemma model acceptance on any route (T015).
- Multi-turn memory behavior with a live model (T026, quickstart scenario 2).
- Off-topic refusal and "I don't know" behavior of the system prompt (T044, SC-002 and SC-003).
- A browser run of the UI against the backend. The frontend was built and type-checked, not run.
