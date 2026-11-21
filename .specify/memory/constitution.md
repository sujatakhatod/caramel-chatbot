<!--
SYNC IMPACT REPORT (temporary scratch for human review; remove before committing)
- Version change: none (no prior constitution) → 1.0.0 (initial ratification)
- Principles (template slots → new titles):
  - [PRINCIPLE_1] → I. Grounded Support Persona
  - [PRINCIPLE_2] → II. Secrets Stay Server-Side
  - [PRINCIPLE_3] → III. Streaming and Latency
  - [PRINCIPLE_4] → IV. Resilient and Privacy-Preserving Operations
  - [PRINCIPLE_5] → V. Simplicity Within MVP Scope
- Added sections: Technology and Security Constraints; Development Workflow and Quality Gates; Governance
- Removed sections: none (template SECTION_2/SECTION_3 renamed to the two sections above)
- Deferred items (TODO):
  - PRD §10: Spring AI integration route (Google GenAI starter vs. OpenAI-compatible endpoint) is unresolved; must be confirmed before wiring.
  - PRD §10: Escalation path for unresolved issues (email, ticket link) is undefined.
  - Build, lint, and test commands do not exist yet; add to CLAUDE.md when scaffolding lands.
-->

# Caramel AI Tech Support Chat Assistant Constitution

## Core Principles

### I. Grounded Support Persona

- The assistant MUST limit answers to LLM inferencing support: API usage, model availability,
  rate limits, latency, errors, billing, and integration.
- Off-topic requests MUST be politely declined, with a pointer back to supported topics.
- The assistant MUST NOT invent pricing, SLAs, model availability, or rate-limit figures.
- When the assistant lacks verified information, it MUST say so. Until the escalation path
  (PRD §10) is defined, it MUST NOT imply that a human will follow up.
- Tone MUST be concise, friendly, and technical.
- The system prompt is the source of truth for persona and boundaries. Any change to its wording
  MUST be treated as a behavior change and re-checked against the PRD §9 success metrics.

**Rationale**: Fabricated pricing or SLAs create customer-facing commitments the company has not made.

### II. Secrets Stay Server-Side

- The Google AI Studio API key (`GOOGLE_API_KEY`) MUST live only in the root `.env` file or the
  process environment, and MUST be loaded by the backend.
- The key MUST NOT be sent to the browser, returned in any response, or written to logs, error
  messages, or traces.
- `.env` MUST be listed in `.gitignore`. A `.env.example` with placeholder values MUST be
  committed instead.
- The model name MUST be configured in `application.yml`, not hardcoded.
- Credentials found in local config (for example, the token in `.mcp.json`) MUST NOT be copied
  into other files or commit messages. They MUST move to environment variables or be removed from
  version control.

**Rationale**: A leaked provider key is billable and cannot be recalled once it reaches a client
or a repository.

### III. Streaming and Latency

- Replies MUST be streamed to the client over SSE. The backend MUST NOT buffer the full completion
  before sending it.
- The first token MUST be visible to the user in under 3 seconds under normal conditions.
- The UI MUST show a streaming indicator from request send until the reply completes or fails.

**Rationale**: Perceived responsiveness is the main latency measure for a chat interface; buffering
would break the 3-second target even when total generation time is acceptable.

### IV. Resilient and Privacy-Preserving Operations

- Provider timeouts and outages MUST produce a user-friendly message in the UI. Raw stack traces
  or provider error payloads MUST NOT reach the user.
- The backend MUST log request metadata, latency, and errors.
- Logs MUST NOT contain user message content or conversation transcripts.
- The backend MUST enforce an input length limit and rate limiting on `POST /api/chat`.
- CORS MUST allow only the frontend origin.

**Rationale**: Operators need latency and error signals to diagnose outages, while customer
messages may contain account or billing details that must not spread into log storage.

### V. Simplicity Within MVP Scope

- Conversation memory MUST be in-process and scoped to a single session. Memory MUST NOT persist
  across sessions.
- The following are out of scope for the MVP and MUST NOT be implemented: user authentication and
  accounts, live human handoff or ticketing integration, persistent chat history, and RAG over
  company documents.
- Adding any out-of-scope item requires a constitution-compliant amendment and a scoped spec. It
  MUST NOT be introduced as an incidental implementation detail.
- The simplest implementation that meets the PRD MUST be chosen. A new dependency MUST be
  justified in the plan that introduces it.

**Rationale**: The PRD defines a narrow MVP. Scope creep in a customer-facing support tool
increases the surface for errors, data retention, and security issues.

## Technology and Security Constraints

- **Backend**: Java with Spring Boot. A Spring AI `ChatClient` with a chat memory advisor holds
  per-session history keyed by session ID.
- **Frontend**: React with TypeScript, built with Vite. The session ID is generated on the client
  and sent with each request.
- **Model**: `gemma-4-26b-a4b-it` served through Google AI Studio.
- **Integration route**: The choice between the Google GenAI starter and the OpenAI-compatible
  endpoint is unresolved (PRD §10). It MUST be confirmed and recorded before the model is wired up.
- **Endpoint contract**: `POST /api/chat` accepts a session ID and user message, and responds
  with an SSE stream.
- **Configuration**: Secrets come from `.env`. Non-secret settings, including the model name, live
  in `application.yml`.

## Development Workflow and Quality Gates

- Work MUST follow the PRD §8 milestone order unless a spec documents a deviation.
- Each milestone MUST be verified against the relevant PRD §9 success metrics before the next
  milestone begins.
- MVP acceptance MUST include manual scenarios for: on-topic questions, off-topic refusal,
  multi-turn context within a session, streaming, and a simulated provider failure.
- Any system prompt change MUST be checked before and after the change against an off-topic
  scenario and a pricing or SLA question the assistant cannot verify.
- Before each commit, the change MUST be checked to confirm that `.env` and credential files are
  not staged.
- The README MUST include run instructions, including how to create `.env` from `.env.example`
  (PRD milestone 6).
- Build, lint, and test commands do not yet exist. They MUST be added to `CLAUDE.md` when
  scaffolding lands, and this constitution MUST NOT assume them before then.

## Governance

- This constitution governs engineering practice for the project. The PRD (`prd.md`) is the source
  of truth for scope and functional requirements. If the two conflict, one MUST be amended
  explicitly. Neither may be overridden silently.
- **Amendment procedure**: An amendment is proposed as a change to this file with a written
  rationale and a Sync Impact Report. It MUST be reviewed and approved by the project owner before
  it takes effect. The version and amendment date MUST be updated in the same change.
- **Versioning policy** (semantic versioning):
  - MAJOR: a principle is removed or redefined in a backward-incompatible way.
  - MINOR: a principle or section is added, or guidance is materially expanded.
  - PATCH: clarifications, wording fixes, or non-semantic refinements.
- **Compliance review**: Every spec, plan, and pull request MUST be checked against all
  principles. A violation MUST either be removed or justified in writing in the plan's complexity
  or justification section. Unjustified violations MUST block merge.
- **Runtime guidance**: `CLAUDE.md` provides day-to-day development guidance and MUST remain
  consistent with this constitution.

**Version**: 1.0.0 | **Ratified**: 2026-11-21 | **Last Amended**: 2026-11-21
