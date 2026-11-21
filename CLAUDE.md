# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

Scaffolded and implemented (feature `specs/001-psec`). The backend is `backend/` (Spring Boot 4.1.3, Spring AI 2.1.0, Java 17+). The frontend is `frontend/` (Vite, React, TypeScript). `prd.md` remains the source of truth for scope. Open item O1 is not resolved: `gemma-4-26b-a4b-it` has not been confirmed on a live call, so live model behavior is unverified (see `specs/001-psec/research.md` R1).

## Commands

Run from the repository root unless noted. The backend needs a `.env` at the root (copy `.env.example`).

- **Backend run**: `cd backend && mvn spring-boot:run` (listens on port 8081, see `server.port`)
- **Backend build and compile check**: `cd backend && mvn -B compile`
- **Frontend dev server**: `cd frontend && npm install && npm run dev` (port 5173)
- **Frontend type-check and build**: `cd frontend && npm run build`
- **Tests**: none yet. No automated tests were requested in the spec. Acceptance uses the manual scenarios in `specs/001-psec/quickstart.md`.

The frontend calls the backend at `VITE_API_BASE_URL` (default `http://localhost:8081`). The backend accepts browser requests only from `chat.allowed-origin` (default `http://localhost:5173`).

## What this is

Caramel AI Tech Support Chat Assistant: a web chat UI where users ask technical questions about Caramel AI's LLM inference API (usage, model availability, rate limits, latency, errors, billing, integration). The assistant is an LLM persona limited to those topics. See `prd.md` for full requirements.

## Planned architecture

```
React + TypeScript (Vite)  --SSE/HTTP-->  Spring Boot + Spring AI  -->  Google AI Studio (gemma-4-26b-a4b-it)
```

- **Backend**: Java, Spring Boot. A `ChatClient` with a chat memory advisor holds per-session conversation history, keyed by session ID. `POST /api/chat` takes a session ID and user message and streams the reply over SSE.
- **Frontend**: React + TypeScript. Single chat window with message list, input, streaming indicator, error state, and a "New chat" action. Session ID is client-generated and sent with each request.
- **Model access**: Spring AI's Google AI Studio integration. Whether to use the Google GenAI starter or the OpenAI-compatible endpoint is still open (see `prd.md` section 10); confirm before wiring it up.
- **Memory is in-process and per-session only.** No persistence across sessions and no user accounts (both out of scope for MVP).

## Constraints that shape the code

- **Secrets**: the Google API key lives in a root `.env` as `GOOGLE_API_KEY=...`, loaded by the backend (Spring Boot can import it via `spring.config.import`). It must never reach the browser or logs. `.env` must be gitignored; commit `.env.example` with a placeholder instead.
- **Model name** is configured in `application.yml`, not hardcoded.
- **System prompt** defines the persona and boundaries. The assistant must not invent pricing, SLAs, or model availability, and should say when it doesn't know. Changes to prompt wording are behavior changes; treat them as such.
- **Security/ops requirements**: CORS restricted to the frontend origin; input length limit; rate limiting; log requests, latency, and errors without logging user message content.
- **Latency target**: first token visible in under 3 seconds under normal conditions. Stream, don't buffer.
- **Errors**: provider timeouts and outages must produce a user-friendly message in the UI, not a raw stack trace.

## Local config notes

- `.mcp.json` contains a GitHub bearer token in plaintext. Don't copy it into other files or commit messages, and it should be moved to an environment variable or removed from version control.
