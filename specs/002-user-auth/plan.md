# Implementation Plan: User Authentication for the Caramel AI Tech Support Chat

**Branch**: `002-user-auth` (directory name used; the repo is git-initialized on `main`, so this is a planned branch name) | **Date**: 2026-11-28 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from [spec.md](spec.md), PRD [../002-prd-user-auth.md](../002-prd-user-auth.md), and the [constitution](../../.specify/memory/constitution.md).

## Summary

Only signed-in customers may use the chat assistant. Customers are invited by Caramel AI staff (assumption recorded below) and sign in with email and password. The backend holds a server-side session, issued as an HTTP-only cookie. Each chat session is created by the server and bound to the signed-in user. `POST /api/chat` rejects anyone without a valid session and anyone using a session they do not own. Idle and absolute session lifetimes, single-use password reset links, and per-account lockout are added. Rate limits move from per-session to per-user, with per-IP limits on sign-in. The assistant's persona, system prompt, and streaming behavior do not change.

Technical approach (details in [research.md](research.md)):

- Spring Security for authentication, with form-style sign-in on a JSON endpoint.
- Spring Session backed by JDBC, so sessions survive a restart and can be revoked server-side.
- Users, sessions, chat-session ownership, and one-time tokens in a relational database. PostgreSQL for deployment; an embedded database for local development.
- BCrypt password hashing via Spring Security's delegating encoder.
- Email via Spring Mail for password reset and invitation delivery. The provider is still open (spec assumption).

## Assumptions Recorded During Planning

- **Sign-up is invitation-only (PRD 10.1 option B).** Resolves spec FR-011 as the PRD recommended. Caramel AI staff create invitations. A customer accepts an invitation by setting a password, which also verifies their email. No public registration endpoint exists. How staff create invitations (a command-line runner or direct database insert) is an operational choice for implementation; no admin UI is in scope.
- **Server-side sessions with cookies (PRD 10.3).** Chosen over JWT because sessions can be revoked on sign-out and when a lockout occurs.
- **Staff use customer accounts for testing**, unless a separate decision is recorded (PRD 10.4 still open).
- **Chat history is not persisted** across restarts (PRD 10.5; memory stays in-process as in the MVP).

## Technical Context

**Language/Version**: Java 17 or later (backend, as in `001-psec`); TypeScript (frontend).

**Primary Dependencies**: Spring Boot 4.1.x; Spring Security; Spring Session JDBC; Spring JDBC; Spring Mail; a migration tool supported by Spring Boot (Flyway); existing Spring AI 2.1.0 chat stack from `001-psec`; React with Vite.

**Storage**: Relational database. PostgreSQL for deployment; embedded database (H2 in PostgreSQL compatibility mode) for local development. Chat memory remains in-process.

**Testing**: JUnit 5 with Spring Boot test slices and an embedded database for backend auth and ownership tests. Manual acceptance scenarios in [quickstart.md](quickstart.md). Frontend tests remain as proposed in `001-psec` (not yet set up).

**Target Platform**: Linux server for the backend; modern desktop and mobile browsers for the frontend.

**Project Type**: Web application (backend API plus frontend SPA), same as `001-psec`.

**Performance Goals**: Sign-in completes in under 1 second at p95 under normal load (spec SC-003 target: 10 seconds on first attempt includes user typing). First chat token within 3 seconds, unchanged (PRD section 6).

**Constraints**:
- Session cookie is `HttpOnly`, `Secure` outside localhost, and `SameSite=Lax` or stricter.
- CSRF protection on state-changing requests that use the cookie.
- CORS allows only the frontend origin, with credentials; no wildcard origin.
- Passwords and tokens are never logged; reset and invitation tokens are stored only as hashes.
- Google API key remains server-side (constitution II).

**Scale/Scope**: Small customer base for launch; design supports several backend instances, so rate-limit and session state must be shared, not only in memory.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Design response | Status |
|-----------|-----------------|--------|
| I. Grounded Support Persona | No change to system prompt, topics, or wording. Auth does not alter assistant behavior (spec FR-013). | PASS |
| II. Secrets Stay Server-Side | Session signing or database credentials come from `.env` or the environment. No new secrets reach the browser. | PASS |
| III. Streaming and Latency | `POST /api/chat` keeps SSE streaming. Authentication adds one session lookup per request, which must not buffer the stream. | PASS (verify in SC acceptance) |
| IV. Resilient and Privacy-Preserving Operations | Auth events logged without passwords, tokens, or message text (spec FR-012). Lockout and reset messages do not reveal account existence. | PASS |
| V. Simplicity Within MVP Scope | **Violation**: the constitution lists user authentication and accounts, persistent history, and a database-backed store as out of scope for the MVP, and forbids implementing them. | **FAILS until the constitution is amended** (see Complexity Tracking) |

**Result**: FAIL on principle V. Implementation must not start until the constitution is amended to move user authentication and accounts from the MVP out-of-scope list into a Phase 2 scope. This is a governance decision for the project owner.

## Open Items

| ID | Item | Status | Blocks | Owner |
|----|------|--------|--------|-------|
| A1 | Constitution principle V and its MVP out-of-scope list forbid authentication and accounts. Amendment needed. | **OPEN** | Implementation start | Project owner |
| A2 | Email provider and sending domain (PRD 10.2). | **OPEN** | Invitation, verification, and reset delivery | Project owner |
| A3 | Staff access model (PRD 10.4): shared customer accounts or separate staff accounts. | **OPEN** | Quickstart test accounts | Project owner |
| A4 | Idle and absolute session durations, and lockout thresholds. | **DEFAULT ADOPTED**: idle 30 minutes, absolute 12 hours; lockout after 5 failures in 15 minutes, locked 15 minutes (spec FR-005, FR-009). | Milestone 3 and 7 | Implementer |
| A5 | Multi-instance rate limiting store. | **DEFAULT ADOPTED**: shared database-backed counters for per-user and per-IP limits (research R6). | Milestone 7 | Implementer |
| A6 | Locked accounts get the same "Email or password is incorrect" message as wrong passwords, so a lock does not reveal account existence (spec FR-009). Customers are not told their account is locked. | **OPEN**: confirm this trade-off or allow a generic "Sign-in is temporarily unavailable" message for all attempts during a lock. | Milestone 7 | Project owner |

## Project Structure

### Documentation (this feature)

```text
specs/002-user-auth/
├── spec.md
├── plan.md                  # This file
├── research.md              # Phase 0 output
├── data-model.md            # Phase 1 output
├── quickstart.md            # Phase 1 output
├── contracts/
│   ├── auth-api.md          # Sign-in, sign-out, session, reset, invitation endpoints
│   └── chat-api-changes.md  # Changes to POST /api/chat from 001-psec
├── checklists/requirements.md
└── tasks.md                 # Generated by /speckit-tasks (not created here)
```

### Source Code (repository root)

Additions to the existing layout from `001-psec`:

```text
backend/src/main/java/com/caramel/chat/
├── auth/                    # NEW: sign-in, sign-out, session, reset, invitation
│   ├── api/                 # Auth controllers and request/response types
│   ├── service/             # Account lifecycle, lockout, token handling
│   ├── domain/              # Account, invitation, reset token, session ownership
│   └── security/            # Spring Security configuration and filters
├── api/ChatController.java  # CHANGED: requires an authenticated user
├── service/ChatService.java # CHANGED: verifies session ownership before memory lookup
└── web/SessionRateLimiter.java # CHANGED: keyed per user and per IP, shared store

backend/src/main/resources/
└── db/migration/            # NEW: schema for accounts, sessions, tokens, chat-session owners

frontend/src/
├── pages/SignInPage.tsx     # NEW: sign-in, reset request, reset confirm, invitation accept
├── components/SignOutButton.tsx # NEW
└── services/authClient.ts   # NEW: session status, expiry handling
```

**Structure Decision**: Extend the existing `backend` and `frontend` web application with an `auth` package and frontend auth pages. No new service or repository is introduced.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Constitution principle V: authentication, accounts, and a database are added after the MVP. | The spec requires that only known customers can use the assistant and that conversations and limits belong to a user. | Keeping anonymous access does not meet the feature goal. Leaving the constitution unchanged would make the feature non-compliant. The amendment (A1) is the simpler path. |
| Relational database added to the stack. | Accounts, password hashes, one-time tokens, and session ownership must survive restarts and be shared across instances. | In-process storage loses accounts on restart and cannot be shared across instances. A file store adds consistency problems without benefit. |
| Spring Session JDBC for server-side sessions. | Sign-out and lockout must invalidate sessions immediately. | Stateless JWTs cannot be revoked before expiry without a server-side list, which would reintroduce the same store. |
