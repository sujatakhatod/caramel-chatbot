# Tasks: User Authentication for the Caramel AI Tech Support Chat

**Input**: Design documents from `specs/002-user-auth/`: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/auth-api.md](contracts/auth-api.md), [contracts/chat-api-changes.md](contracts/chat-api-changes.md), [quickstart.md](quickstart.md)

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Included. The plan's testing approach requires JUnit 5 tests for auth and ownership behavior (plan, Technical Context). Manual acceptance uses [quickstart.md](quickstart.md).

**Organization**: Tasks are grouped by user story so each story can be implemented and tested independently.

## Blocking items

- **A1 (constitution)**: principle V forbids authentication and accounts. Task T005 amends it. **No implementation task may start before T005 is done.**
- **A2 (email provider)**: tasks marked `(blocked by A2)` need a real provider and sending domain. They can be written with a local SMTP sink for testing.
- **A3 (staff access)**: tasks marked `(blocked by A3)` need the staff access decision.
- **A6 (lock message)**: T039 implements the decision in the plan (same generic message for locked accounts). Change it only if the owner rejects A6.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on incomplete tasks)
- **[Story]**: user story label (US1, US2, US3). Setup, Foundational, and Polish tasks have no story label.

## Path Conventions

Web application, per [plan.md](plan.md): backend under `backend/src/main/java/com/caramel/chat/`, frontend under `frontend/src/`, all paths from the repository root.

---

## Phase 1: Setup

**Purpose**: Dependencies and configuration needed by every story.

- [ ] T001 Add backend dependencies to `backend/pom.xml`: Spring Security, Spring JDBC, Spring Session JDBC, Spring Mail, Flyway (Spring Boot support), H2 (runtime, local), PostgreSQL JDBC driver (runtime), and `spring-security-test` (test scope)
- [ ] T002 [P] Add datasource and session settings to `backend/src/main/resources/application.yml`: a local profile using H2 in PostgreSQL compatibility mode, a deployment profile reading the PostgreSQL URL and credentials from the environment, `spring.session.store-type: jdbc`, idle timeout 30 minutes, absolute maximum 12 hours (plan A4)
- [ ] T003 [P] Add mail settings to `backend/src/main/resources/application.yml` and placeholder entries to `.env.example` for SMTP host, port, username, and password, with no values committed (secrets rule). (blocked by A2 for real provider values)
- [ ] T004 [P] Document the local SMTP sink setup in `README.md` so reset and invitation links are read from the sink, not logs (research R5)

---

## Phase 2: Foundational (blocking prerequisites)

**Purpose**: Core auth infrastructure that every user story depends on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete. T005 must be first.

- [ ] T005 Amend `.specify/memory/constitution.md`: move user authentication and accounts out of the MVP out-of-scope list and into a Phase 2 scope under principle V; update the version and the SYNC IMPACT REPORT. Gate A1 (plan, Constitution Check). **Must be the first task in this phase.**
- [ ] T006 Create the Flyway migration `backend/src/main/resources/db/migration/V1__auth_schema.sql` with tables `app_user`, `account_token`, and `chat_session` as in [data-model.md](data-model.md), plus the Spring Session JDBC schema. Constraints from data-model.md: `email` is "Required. Stored lowercased and trimmed. Unique."; `token_hash` is "Required. SHA-256 of the random token. The raw token is never stored."; `purpose` is one of `INVITATION`, `PASSWORD_RESET`.
- [ ] T007 [P] Create `backend/src/main/java/com/caramel/chat/auth/domain/AppUser.java` with the Account fields from data-model.md: `id`, `email` (lowercased, trimmed, unique), `password_hash` (never exposed), `email_verified_at`, `failed_sign_in_count`, `failed_sign_in_window_start`, `locked_until`, `created_at`
- [ ] T008 [P] Create `backend/src/main/java/com/caramel/chat/auth/domain/AccountToken.java` with `purpose` (enum `INVITATION`, `PASSWORD_RESET`), `token_hash`, `expires_at`, `used_at`, and `user_id`
- [ ] T009 [P] Create `backend/src/main/java/com/caramel/chat/auth/domain/ChatSessionOwner.java` with `session_id` (unique), `user_id` (never changes), `created_at`, `last_activity_at`
- [ ] T010 Create Spring JDBC repositories in `backend/src/main/java/com/caramel/chat/auth/domain/`: `AppUserRepository.java`, `AccountTokenRepository.java`, `ChatSessionOwnerRepository.java` (depends on T007–T009)
- [ ] T011 Create `backend/src/main/java/com/caramel/chat/auth/security/SecurityConfig.java`: require authentication for `/api/chat/**`; permit `/api/auth/sign-in`, `/api/auth/session`, `/api/auth/password-reset/**`, `/api/auth/invitations/accept`; session cookie `HttpOnly`, `Secure` outside localhost, `SameSite=Lax`; CSRF protection for cookie-based state changes (contracts/auth-api.md, Cookie section)
- [ ] T012 Update `backend/src/main/java/com/caramel/chat/config/CorsConfig.java`: allow credentials, allow only the origin in `chat.allowed-origin`, no wildcard origin (contracts/chat-api-changes.md, Cross-origin requests)
- [ ] T013 [P] Create `backend/src/main/java/com/caramel/chat/auth/security/PasswordConfig.java`: BCrypt with strength 12 through `DelegatingPasswordEncoder` (research R1)
- [ ] T014 Create `backend/src/main/java/com/caramel/chat/auth/service/TokenService.java`: generate 256-bit random tokens, store only the SHA-256 hash, validate purpose, expiry (reset 30 minutes, invitation 7 days), and unused state (research R8)
- [ ] T015 Extend `backend/src/main/java/com/caramel/chat/web/ChatExceptionHandler.java` to return friendly JSON for 401, 403, 423-equivalent lockout (as 401 per A6), and 429, with no stack traces or provider payloads (contracts/auth-api.md)

**Checkpoint**: Foundation ready. User stories can start.

---

## Phase 3: User Story 1 - Sign in before using the assistant (Priority: P1) 🎯 MVP

**Goal**: Only signed-in customers can reach the chat. Customers accept an invitation, sign in, sign out, and see a sign-in screen when signed out.

**Independent Test**: [quickstart.md](quickstart.md) Scenarios 1–4 pass.

### Tests for User Story 1

- [ ] T016 [P] [US1] Write `backend/src/test/java/com/caramel/chat/auth/SignInTest.java`: correct credentials return 200 and set the session cookie; a wrong password and an unknown email return the identical 401 body "Email or password is incorrect."
- [ ] T017 [P] [US1] Write `backend/src/test/java/com/caramel/chat/api/ChatAuthGateTest.java`: `POST /api/chat` without a session cookie returns 401 with "Please sign in."

### Implementation for User Story 1

- [ ] T018 [US1] Create `backend/src/main/java/com/caramel/chat/auth/service/AccountService.java`: normalize email (trim, lowercase); verify password; refuse unactivated accounts with the generic 401 (FR-003)
- [ ] T019 [US1] Create `backend/src/main/java/com/caramel/chat/auth/api/AuthController.java` with `POST /api/auth/sign-in` as in contracts/auth-api.md (depends on T018)
- [ ] T020 [US1] Add `POST /api/auth/sign-out` (204, invalidates the session, clears the cookie) and `GET /api/auth/session` (200 with `user.email`, or 401 "Please sign in.") to `backend/src/main/java/com/caramel/chat/auth/api/AuthController.java`
- [ ] T021 [US1] Create `backend/src/main/java/com/caramel/chat/auth/service/InvitationService.java`: accept an invitation token; set the password; activate the account; set `email_verified_at`; invalidate the token after use (data-model.md, Account Token validation)
- [ ] T022 [US1] Add `POST /api/auth/invitations/accept` (204 or 400 with the message in contracts/auth-api.md) to `backend/src/main/java/com/caramel/chat/auth/api/AuthController.java` (depends on T021)
- [ ] T023 [US1] Create `backend/src/main/java/com/caramel/chat/auth/cli/InviteCommand.java`: a command-line runner `invite <email>` that creates an `INVITATION` token (research R7) and hands the link to the mailer. Invitations are staff-created; there is no public endpoint.
- [ ] T024 [US1] Create `backend/src/main/java/com/caramel/chat/auth/mail/InvitationMailer.java` using Spring Mail to send the invitation link (blocked by A2)
- [ ] T025 [US1] Update `backend/src/main/java/com/caramel/chat/api/ChatController.java` so `POST /api/chat` requires an authenticated user whose `email_verified_at` is set; unauthenticated requests get 401 (depends on T011)
- [ ] T026 [P] [US1] Create `frontend/src/services/authClient.ts`: `signIn`, `signOut`, `getSession`, `acceptInvitation`, all with `credentials: 'include'`, mapping errors to the friendly messages from the contract
- [ ] T027 [US1] Create `frontend/src/pages/SignInPage.tsx`: email and password form that shows the generic error text on failure (depends on T026 for the client; uses the same message text)
- [ ] T028 [US1] Create `frontend/src/pages/AcceptInvitationPage.tsx`: reads the token from the URL, collects a new password of at least 12 characters, and calls `acceptInvitation`
- [ ] T029 [US1] Update `frontend/src/main.tsx` and `frontend/src/App.tsx`: route to `SignInPage` when `getSession` returns 401, to `AcceptInvitationPage` for the invitation URL, and to `ChatPage` otherwise
- [ ] T030 [P] [US1] Create `frontend/src/components/SignOutButton.tsx` that calls `signOut` and returns to the sign-in screen
- [ ] T031 [US1] Update `frontend/src/pages/ChatPage.tsx` to render `SignOutButton` and to send chat requests with credentials (depends on T030)
- [ ] T032 [US1] Update `frontend/src/services/chatClient.ts` to send `credentials: 'include'` on the chat request and treat 401 as "please sign in" (depends on T026)

**Checkpoint**: User Story 1 works independently: invitation, sign-in, sign-out, and the gate on chat.

---

## Phase 4: User Story 2 - Keep each conversation tied to its owner (Priority: P1)

**Goal**: Conversations belong to the customer who created them. Customers cannot read or write another customer's conversation. Usage limits apply per customer.

**Independent Test**: [quickstart.md](quickstart.md) Scenario 5 passes, and two signed-in customers cannot access each other's chats.

### Tests for User Story 2

- [ ] T033 [P] [US2] Write `backend/src/test/java/com/caramel/chat/auth/ChatSessionOwnershipTest.java`: customer A cannot send to customer B's session; the 403 body for "not yours" is identical to the body for an unknown session ("This chat is not available.")
- [ ] T034 [P] [US2] Write `backend/src/test/java/com/caramel/chat/api/ChatSessionCreateTest.java`: `POST /api/chat/sessions` returns 201 with a server-issued `sessionId` for a signed-in user and 401 otherwise; client-generated IDs are refused by `POST /api/chat`

### Implementation for User Story 2

- [ ] T035 [US2] Create `backend/src/main/java/com/caramel/chat/auth/service/ChatSessionOwnershipService.java`: create an owner record on session creation; check `user_id` equality before any memory lookup; return "not available" for both missing and foreign sessions (data-model.md, Chat Session Owner rule)
- [ ] T036 [US2] Create `backend/src/main/java/com/caramel/chat/api/ChatSessionController.java` with `POST /api/chat/sessions` (201 with `sessionId`) (depends on T035)
- [ ] T037 [US2] Update `backend/src/main/java/com/caramel/chat/service/ChatService.java` to call the ownership check before the in-process memory lookup, so no other user's history is touched (plan, Source Code: CHANGED)
- [ ] T038 [US2] Update `backend/src/main/java/com/caramel/chat/api/ChatController.java` to pass the authenticated user ID into `ChatService` and return 403 with the contract message for foreign sessions (depends on T035, T037)
- [ ] T039 [US2] Replace the in-memory limiter logic in `backend/src/main/java/com/caramel/chat/web/SessionRateLimiter.java` with shared database-backed counters, keyed by user (20 requests per minute, plan O4) and by source IP (research R6). Rejections keep the existing 429 shape.
- [ ] T040 [P] [US2] Update `frontend/src/services/sessionId.ts` to stop generating session IDs in the browser and keep only the server-issued one returned by `POST /api/chat/sessions`
- [ ] T041 [P] [US2] Update `frontend/src/components/NewChatButton.tsx` so "New chat" calls `POST /api/chat/sessions` and shows the new empty conversation
- [ ] T042 [US2] Update `frontend/src/pages/ChatPage.tsx` to create a session on first load if none exists and use the server-issued ID for every message (depends on T040, T041)

**Checkpoint**: User Stories 1 and 2 both work. This is the minimum secure product: restricted access and private conversations.

---

## Phase 5: User Story 3 - Stay signed in safely and recover access (Priority: P2)

**Goal**: Sessions expire after inactivity or a maximum duration, unsent text is kept, customers can reset a forgotten password, and repeated failed sign-ins temporarily lock the account.

**Independent Test**: [quickstart.md](quickstart.md) Scenarios 6–8 pass.

### Tests for User Story 3

- [ ] T043 [P] [US3] Write `backend/src/test/java/com/caramel/chat/auth/LockoutTest.java`: 5 failures in 15 minutes lock the account for 15 minutes; a correct password during the lock gets the same 401 body as a wrong password (plan A6); the lock clears after `locked_until`
- [ ] T044 [P] [US3] Write `backend/src/test/java/com/caramel/chat/auth/PasswordResetTest.java`: a reset token works once, expires after 30 minutes, and a request for an unknown email returns the same 202 body as a known one

### Implementation for User Story 3

- [ ] T045 [US3] Update `backend/src/main/java/com/caramel/chat/auth/service/AccountService.java` with failure counting in the 15-minute window, `locked_until` after 5 failures, counter reset on success, and deletion of the account's sessions when locked (data-model.md, Sign-in Session)
- [ ] T046 [US3] Create `backend/src/main/java/com/caramel/chat/auth/security/AbsoluteSessionLifetimeFilter.java` to end sessions older than 12 hours from their creation time (plan A4); idle timeout is set in T002
- [ ] T047 [US3] Create `backend/src/main/java/com/caramel/chat/auth/service/PasswordResetService.java`: request always returns the same 202 body; confirm validates the token, sets the new password (12–128 characters), marks the token used, and ends the account's other sessions (contracts/auth-api.md)
- [ ] T048 [US3] Add `POST /api/auth/password-reset/request` and `POST /api/auth/password-reset/confirm` to `backend/src/main/java/com/caramel/chat/auth/api/AuthController.java` (depends on T047)
- [ ] T049 [US3] Create `backend/src/main/java/com/caramel/chat/auth/mail/PasswordResetMailer.java` to send the reset link using Spring Mail (blocked by A2)
- [ ] T050 [P] [US3] Create `frontend/src/pages/ResetPasswordPage.tsx` with a request form and a confirm form that use the same messages as the contract
- [ ] T051 [US3] Update `frontend/src/services/chatClient.ts` so a 401 mid-session shows an "expired, sign in again" notice and keeps the draft (depends on T032)
- [ ] T052 [US3] Update `frontend/src/components/MessageInput.tsx` so unsent text is preserved across the sign-in prompt (FR-006)
- [ ] T053 [P] [US3] Update `frontend/src/components/ErrorBanner.tsx` to show the session-expired notice and the generic sign-in failure text

**Checkpoint**: All three stories work independently.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T054 Audit auth logging in `backend/src/main/java/com/caramel/chat/web/RequestLogger.java` and the auth classes: log sign-up, sign-in success and failure, sign-out, lockout, and reset requests with account ID or hashed email and source IP; never log passwords, tokens, cookies, or message content (FR-012, contracts/auth-api.md Logging)
- [ ] T055 [P] Review cookie flags and CSRF settings in `backend/src/main/java/com/caramel/chat/auth/security/SecurityConfig.java` against contracts/auth-api.md Cookie section and plan Constraints
- [ ] T056 [P] Update `CLAUDE.md` with the auth commands: the invite command (T023), required environment variables (T003), local H2 profile, and how to run the auth tests
- [ ] T057 [P] [A3] Create the staff test accounts decided under open item A3 using the invite command, and update `specs/002-user-auth/quickstart.md` Setup step 3 to match (blocked by A3)
- [ ] T058 Run [quickstart.md](quickstart.md) Scenarios 1–10 against the running app and record results in `specs/002-user-auth/quickstart.md`

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: no dependencies.
- **Foundational (Phase 2)**: depends on Setup. T005 blocks every other task.
- **User Stories (Phases 3–5)**: depend on Foundational. US1 and US2 are both P1. US2 uses the signed-in user from US1, so US1 should finish first; US3 depends on US1 for sign-in and on US2 for session ownership.
- **Polish (Phase 6)**: depends on the stories it covers.

### Story dependencies

- **US1 (P1)**: starts after Phase 2. Independent of the other stories.
- **US2 (P1)**: needs US1's authenticated principal (T025, T038 use it).
- **US3 (P2)**: needs US1 sign-in and US2 ownership for the session-expiry and lock tests.

### Parallel opportunities

- Phase 1: T002, T003, T004 in parallel.
- Phase 2: T007, T008, T009, T013 in parallel after T006.
- US1 tests T016 and T017 in parallel; T026 first, then T027, T028, and T030 in parallel.
- US2 tests T033 and T034 in parallel; frontend T040 and T041 in parallel.
- US3 tests T043 and T044 in parallel; frontend T050 and T053 in parallel.

## Implementation Strategy

### MVP first (US1 + US2)

1. Complete Phase 1 and Phase 2, starting with T005.
2. Complete Phase 3 (US1). Validate with [quickstart.md](quickstart.md) Scenarios 1–4.
3. Complete Phase 4 (US2). Validate with Scenario 5.
4. Stop and review. This is a restricted, private-conversation product that can be shown to a pilot group.

### Incremental delivery

1. Add US3 (Phase 5) for session expiry, password reset, and lockout. Validate with Scenarios 6–8.
2. Run Polish (Phase 6), including the full quickstart pass in T058.

---

## Task counts

- Phase 1 (Setup): 4 tasks
- Phase 2 (Foundational): 11 tasks (T005–T015)
- Phase 3 (US1): 17 tasks (T016–T032)
- Phase 4 (US2): 10 tasks (T033–T042)
- Phase 5 (US3): 11 tasks (T043–T053)
- Phase 6 (Polish): 5 tasks (T054–T058)
- **Total**: 58 tasks

## Format validation

Every task starts with `- [ ]`, has a sequential ID (T001–T058), uses `[P]` only for tasks on different files with no unfinished dependency, carries a `[US1]`, `[US2]`, or `[US3]` label only in Phases 3–5, and names a file path or the document it updates (T058 names quickstart.md).
