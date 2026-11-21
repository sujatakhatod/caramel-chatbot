# Implementation Plan: Caramel AI Tech Support Chat (psec)

**Branch**: `001-psec` (no git repository; directory name used) | **Date**: 2026-11-21 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from [spec.md](spec.md), PRD [prd.md](../../prd.md), and the
[constitution](../../.specify/memory/constitution.md).

## Summary

Build a web chat assistant for Caramel AI's LLM inference API. A React and TypeScript frontend
sends user messages to a Spring Boot backend. The backend uses Spring AI to call
`gemma-4-26b-a4b-it` through Google AI Studio and streams the reply back over SSE. Conversation
memory is per session and in-process. The system prompt keeps the assistant on LLM inference
support and forbids invented pricing, SLAs, or availability.

Technical approach (from [research.md](research.md)): Spring AI Google GenAI starter as the primary
model route, an OpenAI-compatible endpoint as fallback, a `ChatClient` with a message-window memory
advisor keyed by session ID, and `Flux`-based streaming.

## Technical Context

**Language/Version**: Java 17 or later (backend; Spring Boot 4.1.x supports Java 17 through 27)
and TypeScript (frontend). Spring AI 2.1.0 compatibility with this baseline is confirmed at
scaffolding (research R2).

**Primary Dependencies**: Spring Boot 4.1.x (Spring Framework 7.0.11 or later); Spring AI 2.1.0
via BOM (`spring-ai-bom`); `spring-ai-starter-model-google-genai`; React with Vite.

**Storage**: In-process memory only. No database. Conversation history is per session and lost
on restart (spec FR-017).

**Testing**: Proposed: JUnit 5 with Spring Boot test support and a mocked chat model for the
backend; Vitest with React Testing Library for the frontend; manual scenario testing for MVP
acceptance. No tooling exists yet. Commands are added to `CLAUDE.md` at scaffolding.

**Target Platform**: Linux server for the backend; modern desktop and mobile web browsers for the
frontend.

**Project Type**: Web application (backend API plus frontend SPA).

**Performance Goals**: First visible token within 3 seconds under normal conditions (spec
FR-003, SC-001). Stream every token as it arrives.

**Constraints**: API key server-side only. Message at most 2,000 characters. Per-session rate
limit. CORS restricted to the frontend origin. No user message text in logs. No persistence
across sessions.

**Scale/Scope**: MVP. One chat window, a small number of concurrent sessions, four user stories,
18 functional requirements.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Design response | Status |
|-----------|-----------------|--------|
| I. Grounded Support Persona | System prompt restricts topics, forbids invented figures, requires saying "I don't know". Prompt changes are reviewed as behavior changes (Development Workflow). | PASS |
| II. Secrets Stay Server-Side | Key read from `.env` via `spring.config.import` into `spring.ai.google.genai.api-key`. Model name in `application.yml`. Contract forbids keys in responses or error events. | PASS |
| III. Streaming and Latency | `POST /api/chat` returns `text/event-stream` from a `Flux`. Never collected or blocked. Pending indicator in UI. | PASS (verify latency in SC-001 test) |
| IV. Resilient and Privacy-Preserving Operations | `error` SSE event with friendly text. Logs record session ID, latency, outcome; never message text. Length limit (413) and rate limit (429). CORS to frontend origin only. | PASS |
| V. Simplicity Within MVP Scope | In-process memory. No auth, handoff, persistence, or RAG. Chosen dependencies are the minimum: Spring AI starter plus the React stack. | PASS |

**Open gate (not a planning blocker, but a hard precondition for wiring the model)**: The
constitution requires the integration route to be confirmed before the model is wired up. The
Gemma model ID does not appear in Google's documented Gemini model list (research R1). Escalated
as open item O1.

**Result**: PASS for planning. Implementation of PRD milestone 2 is blocked until O1 is resolved.

## Open Items

| ID | Item | Status | Blocks | Owner |
|----|------|--------|--------|-------|
| O1 | Confirm `gemma-4-26b-a4b-it` is accepted by a live call on one route: Google GenAI (AI Studio), OpenAI-compatible (fallback), or Vertex AI through the same starter (candidate, see research R1). | **OPEN**. Needs an API key and one test call, which were not available. | PRD milestone 2 (model wiring) | Project owner |
| O2 | Java and Spring Boot baseline. | **RESOLVED** for planning: Java 17 or later, Spring Boot 4.1.x, Spring Framework 7.0.11 or later (research R2). Spring AI 2.1.0 is re-checked at scaffolding. | PRD milestone 1 | Implementer |
| O3 | Escalation path for unresolved issues (PRD §10). | **DEFAULT ADOPTED**: no escalation contact in the MVP. The assistant states it cannot resolve the issue and never implies a human will follow up (constitution I). Product may replace this later. | Release | Product |
| O4 | Per-session rate limit value (spec assumption). | **RESOLVED**: 20 requests per minute per session, adjustable in `application.yml` (research R5). | Milestone 5 | Implementer |

## Project Structure

### Documentation (this feature)

```text
specs/001-psec/
├── spec.md                 # Feature specification (input)
├── plan.md                 # This file
├── research.md             # Phase 0 output
├── data-model.md           # Phase 1 output
├── quickstart.md           # Phase 1 output
├── contracts/
│   └── chat-api.md         # POST /api/chat contract
├── checklists/
│   └── requirements.md     # Spec quality checklist
└── tasks.md                # Created by /speckit-tasks (not by /speckit-plan)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/.../chat/
│   ├── api/                # ChatController (POST /api/chat, SSE), request/response records
│   ├── service/            # ChatService: ChatClient with memory advisor, session handling
│   ├── config/             # Spring AI and model config, CORS, rate limit, .env import
│   ├── web/                # Input validation, error mapping to user-safe messages
│   └── prompt/             # System prompt resource (reviewed as behavior)
├── src/main/resources/
│   ├── application.yml     # Model name, CORS origin, limits
│   └── prompts/system.md   # System prompt text
└── src/test/java/.../chat/ # Unit tests with mocked chat model; contract tests

frontend/
├── src/
│   ├── components/         # MessageList, MessageInput, StreamingIndicator, ErrorBanner, NewChatButton
│   ├── services/           # chat client: SSE reader, session ID generation
│   └── pages/              # ChatPage
└── tests/                  # Component and client tests

.env.example               # GOOGLE_API_KEY=<placeholder>; committed
.gitignore                 # Must list .env
README.md                  # Run instructions, .env creation (PRD milestone 6)
```

**Structure Decision**: Web application with a separate backend and frontend, as the PRD's
architecture requires. The backend is a single Spring Boot service with feature-oriented packages.
The frontend is a single-page React app with one chat page. No additional projects or services
are introduced, which keeps the structure within constitution V.

## Complexity Tracking

No constitution violations. No entries required.
