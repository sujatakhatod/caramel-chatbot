# Research: Caramel AI Tech Support Chat (001-psec)

**Date**: 2026-11-21

## R1. Model access route for `gemma-4-26b-a4b-it`

**Decision**: Use the Spring AI Google GenAI starter (`spring-ai-starter-model-google-genai`,
properties under `spring.ai.google.genai`) as the primary route. Keep the OpenAI-compatible
endpoint as a documented fallback. Confirm the route with a live call before wiring the model
(constitution, Technology and Security Constraints).

**Rationale**:
- The Spring AI Google GenAI page documents the starter and the Gemini Developer API key property
  (`spring.ai.google.genai.api-key`). It is the native integration for Google AI Studio.
- The Gemini API OpenAI-compatibility page documents base URL
  `https://generativelanguage.googleapis.com/v1beta/openai/` and states streaming is supported.
  The fallback is the Spring AI OpenAI starter (`spring-ai-starter-model-openai`) with
  `spring.ai.openai.base-url` set to that URL.

**Blocking finding**: Neither Google page lists `gemma-4-26b-a4b-it` or any other Gemma model ID.
The Gemini API model list has no Gemma entries. The PRD's model is therefore unverified on this
endpoint. This is the top risk for the plan.

**Follow-up research (2026-11-21)**:
- The model exists. Google's Gemma 4 model card and Hugging Face list `google/gemma-4-26B-A4B-it`,
  and OpenRouter offers it. Google Cloud also lists `gemma-4-26b-a4b-it` on its managed model
  platform (Gemini Enterprise Agent Platform, MaaS).
- No AI Studio page confirmed it.
- **Third candidate route**: Spring AI's Google GenAI starter also supports Vertex AI, configured
  with `spring.ai.google.genai.project-id` and `spring.ai.google.genai.location` (per the Spring AI
  Google GenAI page). Whether Gemma is reachable that way is unverified.
- Sources: [Hugging Face model card](https://huggingface.co/google/gemma-4-26B-A4B-it),
  [Google Cloud model page](https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/maas/google/gemma-4-26b-a4b-it),
  [OpenRouter listing](https://openrouter.ai/google/gemma-4-26b-a4b-it:free).

**Status**: Still unverified by a live call. Requires an API key and one test request, which this
environment does not have. Owner: project owner (open item O1 in plan.md).

**Action**: Before milestone 2 (PRD §8), make one authenticated test call to
`gemma-4-26b-a4b-it` through the chosen route. If the model is rejected on both routes, stop and
escalate to the project owner. Do not silently substitute a Gemini model, because the PRD names
Gemma.

**Alternatives considered**:
- OpenAI-compatible endpoint as primary: same model-ID uncertainty, and it adds a second
  compatibility layer. Kept as fallback only.
- Calling Google's REST API directly: rejected. It bypasses Spring AI and the principle of
  simplicity (constitution V).

## R2. Spring AI version

**Decision**: Manage all Spring AI artifacts through the Spring AI BOM. Pin the latest stable
release confirmed at scaffolding time. The Google GenAI page lists **2.1.0** as the current
stable release.

**Conflict noted**: The local `spring-ai-chatbot` skill reference targets **2.0.2**. The live
documentation shows 2.1.0. The live documentation wins, and API names must be re-verified against
the pinned version's reference, not the skill notes.

**Alternatives considered**: Pin 2.0.2 per the local skill. Rejected because the newer stable
release is the one the official docs present, and the project is starting fresh.

**Java and Spring Boot baseline (resolves O2 for planning)**:
- Spring AI's Getting Started page states that Spring AI 2.0.x supports Spring Boot 4.0.x and 4.1.x.
  It does not state a 2.1.0 requirement.
- Spring Boot 4.1.3 requires at least Java 17 and supports up to Java 27, and requires Spring
  Framework 7.0.11 or later
  ([Spring Boot 4.1 system requirements](https://docs.spring.io/spring-boot/4.1/system-requirements.html)).
- The Spring AI release notes for 2.1.0 do not state a Java or Boot requirement. Confirm Spring AI
  2.1.0 against Boot 4.1.x when the project is scaffolded, before adding dependencies.
- BOM coordinates: `org.springframework.ai:spring-ai-bom`, type `pom`, scope `import`. The docs
  examples show 2.0.0. Use 2.1.0 once confirmed on the Maven repository at scaffolding.

## R3. Streaming over SSE

**Decision**: `POST /api/chat` returns `text/event-stream`. The controller returns a `Flux` from
the ChatClient stream (`.stream().content()`). The stream is never blocked or collected.

**Rationale**: Satisfies constitution III (stream, don't buffer) and spec FR-002 and FR-003.

**Error handling**: Provider errors are mapped to a single friendly SSE error event
(see `contracts/chat-api.md`). Stack traces never reach the client.

## R4. Per-session memory

**Decision**: Use Spring AI `ChatClient` with a message-window chat memory advisor. Key it by the
client-generated session ID. Store it in process memory only.

**Rationale**: Matches spec FR-005 and FR-017 (no persistence across sessions) and constitution V.

**Caveat**: Memory is lost on backend restart. Acceptable for MVP per the PRD's per-session scope.

**Alternatives considered**: A database-backed memory store. Rejected as out of scope (PRD §4).

## R5. Rate limiting and input limits

**Decision**: Enforce a 2,000-character maximum message length (spec assumption) and a per-session
limit of **20 requests per minute**, configurable in `application.yml`. Return HTTP 413 for
oversized input and 429 for rate limiting.

**Rationale for 20 per minute**: Well above normal human chat pace (about one message every few
seconds), so legitimate users are not throttled. Low enough to limit abuse of the provider quota.
Adjust after the MVP sees real traffic.

**Rationale**: Constitution IV and spec FR-012 and FR-013. A per-session key is used because
accounts are out of scope.

## R6. Secrets handling

**Decision**: `GOOGLE_API_KEY` is read from the process environment, populated from the root
`.env` through `spring.config.import`. The key is mapped to `spring.ai.google.genai.api-key`.
The model name lives in `application.yml`.

**Rationale**: Constitution II and PRD §5.

## R7. Testing tooling

**Decision (proposed)**: JUnit 5 and Spring Boot test support for the backend, with the chat
model mocked in unit tests. Vitest with React Testing Library for the frontend. Manual scenario
testing for MVP acceptance (constitution, Development Workflow).

**Status**: Proposed. No tooling exists yet. The concrete commands are added to `CLAUDE.md` when
scaffolding lands.

## Open items carried into design

| Item | Owner | Gate | Status |
|------|-------|------|--------|
| Confirm `gemma-4-26b-a4b-it` works on the chosen route (AI Studio, OpenAI-compatible, or Vertex AI) | Project owner | Before PRD milestone 2 | OPEN: needs API key and a test call |
| Confirm Spring AI 2.1.0 against Spring Boot 4.1.x and Java 17+ | Implementer | Scaffolding (milestone 1) | Baseline documented; 2.1.0 check at scaffolding |
| Escalation path for unresolved issues | Product (PRD §10) | Before release | DEFAULT ADOPTED: no escalation contact in MVP |
| Per-session rate limit value | Implementer | Milestone 5 | RESOLVED: 20 requests per minute (R5) |
