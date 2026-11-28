# Research: User Authentication (002-user-auth)

**Date**: 2026-11-28

Each entry gives the decision, the rationale, and the alternatives considered.

## R1. Password hashing

- **Decision**: BCrypt (cost factor 12) through Spring Security's delegating password encoder, so the scheme can change later.
- **Rationale**: Built into Spring Security with no native dependency. Adaptive cost meets PRD section 6.
- **Alternatives**: Argon2id (stronger, but needs a native library on the JVM, which adds deployment friction). Revisit if the security review asks for it.

## R2. Session mechanism

- **Decision**: Server-side sessions. The session ID travels in an `HttpOnly`, `Secure` (outside localhost), `SameSite=Lax` cookie. Session data is stored by Spring Session JDBC.
- **Rationale**: Sign-out and lockout must end sessions immediately. Server-side sessions allow this directly (PRD 10.3 recommendation).
- **Alternatives**: Stateless JWTs with refresh tokens. Rejected because revocation would need a server-side list anyway.

## R3. Session store and database

- **Decision**: Relational database through Spring JDBC. PostgreSQL in deployment; H2 in PostgreSQL compatibility mode for local development. Schema managed by Flyway migrations.
- **Rationale**: Accounts, tokens, and session ownership must survive restarts and be shared by several instances. PostgreSQL is the common deployment choice; H2 keeps local setup to one command.
- **Alternatives**: Redis for sessions (adds a second data store). Embedded-only storage (loses data on restart).

## R4. Schema migrations

- **Decision**: Flyway migrations under `backend/src/main/resources/db/migration/`, applied on startup.
- **Rationale**: Versioned, repeatable schema changes that work on H2 and PostgreSQL.
- **Alternatives**: Hibernate `ddl-auto` (not safe for deployment).

## R5. Email delivery

- **Decision**: Spring Mail over SMTP, with the sending provider and credentials supplied through `.env` (secrets rule). Local development writes emails to the log in a redacted form, or to a local SMTP test sink, and never includes the link in the normal log stream without redaction.
- **Rationale**: Provider choice is open (PRD 10.2, plan A2). SMTP keeps the code independent of the provider.
- **Alternatives**: A provider-specific HTTP API. Deferred until the provider is chosen.
- **Caveat**: Logging the reset link during local development conflicts with the "never log tokens" rule. Use a local SMTP sink instead, so the link is only in the sink's mailbox.

## R6. Rate limiting across instances

- **Decision**: Per-user and per-IP counters stored in the shared database. The existing in-memory `SessionRateLimiter` in `001-psec` is replaced.
- **Rationale**: Per-process counters let a user bypass limits by hitting different instances. The shared store is the simplest correct option at this scale.
- **Alternatives**: Keep in-memory counters (acceptable only for a single instance). A dedicated rate-limit library with a shared backend (more moving parts for a small customer base).

## R7. Invitation creation

- **Decision**: Invitations are created by a backend command-line runner invoked by staff (for example, `java -jar ... invite <email>`), which writes a single-use invitation token hash and emails the link.
- **Rationale**: Invitation-only sign-up (PRD 10.1 option B) needs a way to create invitations; an admin UI is out of scope in the spec.
- **Alternatives**: An authenticated admin endpoint (requires an admin role, which is out of scope). A manual database insert (error-prone).

## R8. Token handling

- **Decision**: Reset and invitation tokens are random 256-bit values. Only a SHA-256 hash is stored. Tokens are single-use and expire (reset 30 minutes; invitation 7 days).
- **Rationale**: A database leak must not expose usable links.
- **Alternatives**: Storing tokens in plaintext (rejected on security grounds).

## R9. Chat session ownership

- **Decision**: The server issues chat session IDs through `POST /api/chat/sessions`. Each row in a `chat_session` table records its owner's user ID. Every chat request checks ownership before touching the in-process memory keyed by session ID.
- **Rationale**: Client-chosen IDs cannot be trusted to identify ownership (spec FR-007). Checking before memory lookup keeps other users' histories unreachable.
- **Alternatives**: Prefixing the session ID with the user ID (leaks identifiers and still needs a check).
