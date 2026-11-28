# Quickstart: Validate User Authentication

**Date**: 2026-11-28
**Requires**: The 001-psec app running locally (backend on port 8081, frontend on port 5173), and this feature implemented. The live model route (001-psec open item O1) must be confirmed for the chat scenarios to produce real replies.

Run these scenarios in order. Each lists the expected outcome. Contract details are in [contracts/auth-api.md](contracts/auth-api.md) and [contracts/chat-api-changes.md](contracts/chat-api-changes.md).

## Setup

1. Copy `.env.example` to `.env` and set `GOOGLE_API_KEY`, plus the email (SMTP) settings for the local SMTP sink described in research R5.
2. Start the backend and frontend as in `CLAUDE.md`.
3. Create one invitation for a test customer with the operator tool (research R7). Open the link from the local SMTP sink.

## Scenario 1: Signed-out visitor is refused (spec US1, FR-001)

1. Open the chat while signed out.
2. **Expect**: sign-in screen; no message box is active.
3. Call `POST /api/chat` directly without a cookie.
4. **Expect**: `401` with `{"message": "Please sign in."}`.

## Scenario 2: Accept invitation and sign in (spec US1, FR-002, FR-011)

1. Open the invitation link and set a password of at least 12 characters.
2. **Expect**: account activated; you are asked to sign in.
3. Sign in with the email and password.
4. **Expect**: chat window opens; a new chat can be started.

## Scenario 3: Wrong credentials give a generic message (spec FR-003)

1. Sign in with a correct email and a wrong password, then with an unknown email.
2. **Expect**: the same message both times, "Email or password is incorrect."

## Scenario 4: Sign-out ends access (spec FR-004)

1. Sign out.
2. Call `POST /api/chat` with the old cookie.
3. **Expect**: `401`.

## Scenario 5: Conversations belong to their owner (spec US2, FR-007)

1. Sign in as customer A and create a chat. Note its session ID.
2. Sign in as customer B in another browser profile. Send a message using A's session ID.
3. **Expect**: `403` with `{"message": "This chat is not available."}`. Nothing from A's chat is returned.
4. Confirm that a made-up session ID gives the same `403`.

## Scenario 6: Session expiry keeps the draft (spec US3, FR-005, FR-006)

1. Sign in and type a draft message without sending.
2. Wait past the idle timeout (30 minutes), or shorten it temporarily in local configuration.
3. Send the draft.
4. **Expect**: a notice that the session expired and a sign-in prompt; the draft is still in the input after signing in again.

## Scenario 7: Password reset (spec US3, FR-008)

1. Request a reset for the test account. Open the link from the local SMTP sink.
2. Set a new password. **Expect**: success. Sign in with the new password.
3. Open the same link again. **Expect**: "This reset link is no longer valid."
4. Request a reset for an unknown email. **Expect**: the same confirmation message as for a known email.

## Scenario 8: Lockout (spec US3, FR-009)

1. Enter the wrong password 5 times within 15 minutes for the test account.
2. Sign in with the correct password on the next attempt.
3. **Expect**: the same "Email or password is incorrect." message as a wrong password, with no lock notice (plan open item A6).
4. Wait 15 minutes, or reset the lock in the local database. Sign in with the correct password.
5. **Expect**: success.

## Scenario 9: Logs stay clean (spec FR-012, SC-007)

1. Run scenarios 2 through 8.
2. Search the backend log output for the test password, any reset or invitation token, and the text of any chat message.
3. **Expect**: none found. Sign-in, lockout, and reset-request events appear with account ID or hashed email and source IP.

## Scenario 10: Streaming still works (PRD section 6 latency)

1. Signed in, send an on-topic question.
2. **Expect**: first token visible within 3 seconds under normal conditions; the reply streams and ends with `done`.
