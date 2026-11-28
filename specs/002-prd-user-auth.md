# PRD 002: User Authentication for the Caramel AI Tech Support Chat

Written for: engineers and product owners working on the Caramel AI chat assistant, who know the MVP in `prd.md`.

Status: draft. Open questions in section 10 need decisions before implementation.

## 1. Background

The MVP (`prd.md`) has no user accounts. Any visitor can open the chat, and each chat session is identified only by a client-generated session ID. That session ID is not tied to a person, so the backend cannot tell who is asking, cannot tie a conversation to a customer account, and cannot limit usage per customer.

This PRD adds user authentication so that only known Caramel AI customers can use the assistant, and so that each conversation and each rate limit belongs to a specific user.

## 2. Objective

Let registered Caramel AI customers sign in to the chat assistant, so that access is restricted to authenticated users and each user's sessions are linked to their account.

## 3. Target Users

- **Customer developers** who integrate the inference API and need technical help.
- **Customer billing and technical contacts** at client companies.
- **Caramel AI staff** who may need to test the assistant (see open question 10.4).

## 4. Scope

### In scope
- Account sign-in with email and password.
- Sign-out.
- Sign-up or account creation, depending on the decision in open question 10.1.
- Server-side session or token management, so that the frontend never holds the Google AI Studio key or any long-lived credential.
- Linking each chat session to the authenticated user.
- Per-user rate limiting, replacing or extending the per-session limit from the MVP.
- Protecting `POST /api/chat` and any new auth endpoints from unauthenticated access.
- Password reset by email.

### Out of scope
- Single sign-on (SAML, OIDC with corporate identity providers). Candidate for a later phase.
- Role-based access beyond one default customer role (admin tooling is a separate PRD).
- Multi-factor authentication. Candidate for a later phase.
- Organization or team accounts that share a billing contact. Candidate for a later phase.
- Persistent chat history across sign-ins. Memory stays in-process and per-session, as in the MVP, unless open question 10.5 changes this.
- Changes to the system prompt or the model. Auth does not alter assistant behavior.

## 5. Functional Requirements

1. **Sign-up**: a visitor can create an account with an email address and password. The email must be verified before the account can call the chat endpoint. (Depends on 10.1.)
2. **Sign-in**: a user signs in with email and password and receives an authenticated session. Failed attempts return a generic message ("Email or password is incorrect") that does not reveal whether the email exists.
3. **Sign-out**: a user can end the authenticated session. The server invalidates it, and the frontend drops its credentials.
4. **Session expiry**: authenticated sessions expire after a fixed idle period and after an absolute maximum lifetime. The user is shown a sign-in prompt when this happens, and the current draft is preserved if possible.
5. **Protected endpoints**: `POST /api/chat` returns `401 Unauthorized` for requests without a valid authentication. The frontend shows the sign-in screen in response.
6. **Session ownership**: every chat session ID is bound to the user who created it. A user cannot send messages into another user's session, and such attempts return `403 Forbidden` (or `404` to avoid revealing that the session exists).
7. **Per-user rate limiting**: the existing rate limit is applied per authenticated user, with a per-IP limit still applied to sign-in and sign-up to slow down credential stuffing.
8. **Password reset**: a user can request a reset link by email. Reset links are single-use and expire after a short window (proposed: 30 minutes).
9. **Account lock**: after repeated failed sign-in attempts for one account, sign-in is temporarily blocked (proposed: 5 failures in 15 minutes, then a 15-minute lock). The lock message must not reveal account existence.
10. **Frontend**: a sign-in and sign-up view, a sign-out control in the chat window, and a clear message for expired sessions and lockouts. The existing "New chat" button still works and creates a new session bound to the current user.

## 6. Non-Functional Requirements

- **Password storage**: passwords are hashed with a slow adaptive algorithm (bcrypt, scrypt, or Argon2id, as provided by Spring Security). Plaintext passwords are never stored or logged.
- **Credentials in transit**: all auth traffic uses HTTPS in any non-local deployment. Local development may use HTTP on `localhost`.
- **Cookies or tokens**: if session cookies are used, they are `HttpOnly`, `Secure` (outside localhost), and `SameSite=Lax` or stricter. CORS remains limited to the frontend origin, and credentialed CORS requests must name that origin explicitly, not `*`.
- **CSRF**: state-changing requests are protected against CSRF when cookie-based sessions are used.
- **Secrets**: no new secrets are stored in the frontend. Any signing key or session secret is loaded from `.env` or the environment, following the MVP rule in `prd.md` section 5.
- **Logging**: auth events are logged (sign-up, sign-in success and failure, sign-out, lockout, password reset requested), each with the user ID or a hashed email and the source IP. Passwords, reset tokens, session tokens, and message content are never logged.
- **Latency**: sign-in completes in under 1 second at p95 under normal load. The chat latency target in `prd.md` (first token under 3 seconds) still applies after authentication.
- **Availability**: an email provider outage must not block sign-in for already verified users. Only sign-up verification and password reset depend on email.

## 7. Architecture Notes

- Use Spring Security for authentication, with Spring Session or a signed, server-side session store. Choose one in the plan phase.
- The chat session ID from the MVP becomes a server-issued identifier bound to the user, rather than a value the client alone chooses.
- Storage for users and password hashes needs a database. Options: an embedded database for local development, PostgreSQL for deployment. This is a new dependency and must be called out in the plan.
- Chat memory remains in-process and keyed by session ID. Session ownership checks happen before any memory lookup.
- Rate limiting moves from per-session to per-user and per-IP. The limiter must be shared across requests, not only in one process, once more than one instance runs.

## 8. Milestones

1. Decisions on open questions 10.1 to 10.3.
2. User storage, password hashing, and sign-up/sign-in endpoints, with tests.
3. Session management, sign-out, and expiry.
4. Protect `POST /api/chat` and bind sessions to users.
5. Frontend sign-in, sign-up, and expiry handling.
6. Email verification and password reset.
7. Lockout and per-user rate limiting.
8. Security review of auth flows, then manual acceptance scenarios added to `specs/001-psec/quickstart.md` or a new quickstart for this feature.

## 9. Success Metrics

- Unauthenticated requests to `POST /api/chat` are rejected in 100% of test cases.
- A user cannot read or write another user's session in any test case.
- Password reset and sign-in work end to end in manual acceptance tests.
- No credential or message content appears in logs, verified by reviewing log output during acceptance testing.

## 10. Open Questions

1. **Sign-up model:** Do customers self-register, or are accounts created by Caramel AI staff and invited by email? Self-registration is simpler; invitation-only limits access to paying customers. (Recommendation: invitation-only for MVP of auth, since the assistant is a customer support tool.)
2. **Email provider:** which service sends verification and reset emails, and who owns the sending domain?
3. **Session mechanism:** server-side session cookies or short-lived access tokens (JWT) with refresh? (Recommendation: server-side sessions with cookies, because they are easier to revoke.)
4. **Staff access:** do Caramel AI staff sign in to test the assistant with the same accounts as customers, or with separate accounts?
5. **Chat history:** should a user's conversations survive sign-out and sign-in? This PRD keeps memory in-process, so history is lost on server restart. Persisting history is a separate decision.
6. **Account data:** which fields are needed beyond email and password (name, company, plan)? Keep this minimal unless billing requires more.
7. **Lockout thresholds:** are the proposed values in requirement 9 acceptable?
