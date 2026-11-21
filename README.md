# Caramel AI Tech Support Chat

A chat assistant for Caramel AI's LLM inference API. It answers technical questions about usage, model availability, rate limits, latency, errors, billing, and integration. The answers come from Gemma (`gemma-4-26b-a4b-it`) through Google AI Studio.

- `backend/`: Spring Boot 4.1.3 with Spring AI 2.1.0. Exposes `POST /api/chat`, which streams the reply as Server-Sent Events.
- `frontend/`: Vite, React, and TypeScript. A single chat window with streaming, errors, and a "New chat" action.

## Prerequisites

- Java 17 or later and Maven 3.9 or later
- Node.js with npm
- A Google AI Studio API key

## 1. Create `.env`

The API key is read from a root-level `.env` file. The file is gitignored and must never be committed.

```bash
cp .env.example .env
```

Edit `.env` and replace the placeholder:

```text
GOOGLE_API_KEY=<your Google AI Studio key>
```

## 2. Run the backend

```bash
cd backend
mvn spring-boot:run
```

The backend listens on port 8081. Change `server.port` in `backend/src/main/resources/application.yml` if needed. Startup fails fast if no key is available.

## 3. Run the frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The backend accepts browser requests only from `chat.allowed-origin` (default `http://localhost:5173`).

To point the frontend at a different backend, set `VITE_API_BASE_URL` before starting the dev server.

## Configuration

Non-secret settings live in `backend/src/main/resources/application.yml`:

| Key | Default | Purpose |
|-----|---------|---------|
| `spring.ai.google.genai.chat.model` | `gemma-4-26b-a4b-it` | Model name |
| `chat.max-message-length` | `2000` | Maximum characters per message |
| `chat.rate-limit-per-minute` | `20` | Requests per session per minute |
| `chat.allowed-origin` | `http://localhost:5173` | Only allowed browser origin |
| `chat.memory-window-size` | `20` | Messages kept per session |
| `chat.provider-idle-timeout` | `30s` | Fails a reply if no token arrives in this time |

The system prompt is in `backend/src/main/resources/prompts/system.md`. Changes to it change the assistant's behavior, so review them carefully.

## Status and known limits

- **Model access is not yet verified.** The model ID `gemma-4-26b-a4b-it` has not been confirmed with a live call on the chosen route. See `specs/001-psec/research.md` (R1) and open item O1 in `specs/001-psec/plan.md`.
- Conversation history is kept in memory for each browser session. It is lost on backend restart and is never persisted.
- There are no user accounts, no human handoff, and no document-based answers in this MVP.
- Feature specification, plan, and tasks: `specs/001-psec/`.
