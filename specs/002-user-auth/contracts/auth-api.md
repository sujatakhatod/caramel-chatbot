# Contract: Auth API

**Date**: 2026-11-28
**Consumer**: The React frontend, in the browser.
**Provider**: The Spring Boot backend.

All request and response bodies are JSON. Errors use the same friendly shape as the chat API (`{"message": "..."}`); no stack traces or provider payloads are returned.

## POST /api/auth/sign-in

**Request**: `{"email": "<string>", "password": "<string>"}`

| Status | When | Body |
|--------|------|------|
| `200 OK` | Valid credentials, account active and not locked. Sets the session cookie. | `{"user": {"email": "<string>"}}` |
| `401 Unauthorized` | Email unknown, password wrong, account not yet activated, **or account temporarily locked**. | `{"message": "Email or password is incorrect."}` |
| `429 Too Many Requests` | Per-IP sign-in limit exceeded. | `{"message": "Too many attempts. Try again later."}` |

**Rules**:
- Every credential failure returns the same `401` body, including locked accounts. A distinct lock response would reveal that an email is registered, which spec FR-003 and FR-009 forbid. Trade-off: a legitimate customer locked out sees the same "incorrect" message, not a "locked" notice. This is recorded as open item A6 in the plan and needs owner confirmation.
- The lock clears automatically when `locked_until` passes.
- The `429` per-IP limit is keyed by source IP, not by email, so it does not reveal account existence either.

## POST /api/auth/sign-out

Requires a session. Invalidates the server session and clears the cookie.

| Status | When |
|--------|------|
| `204 No Content` | Signed out. |
| `401 Unauthorized` | No valid session. |

## GET /api/auth/session

Returns the current signed-in state. Used by the frontend on load.

| Status | Body |
|--------|------|
| `200 OK` | `{"user": {"email": "<string>"}}` |
| `401 Unauthorized` | `{"message": "Please sign in."}` |

## POST /api/auth/password-reset/request

**Request**: `{"email": "<string>"}`

| Status | Body |
|--------|------|
| `202 Accepted` | Always, whether or not the email is registered: `{"message": "If that email has an account, a reset link is on its way."}` |
| `429 Too Many Requests` | Per-IP or per-email limit exceeded. Same message. |

**Rule**: the response is identical in content and timing class whether or not the account exists (spec FR-003).

## POST /api/auth/password-reset/confirm

**Request**: `{"token": "<string>", "newPassword": "<string>"}`

| Status | When | Body |
|--------|------|------|
| `204 No Content` | Token valid; password changed; token marked used; the account's other sessions are ended. | none |
| `400 Bad Request` | Token invalid, expired, or already used. | `{"message": "This reset link is no longer valid. Request a new one."}` |
| `400 Bad Request` | Password fails the password rules. | `{"message": "<rule explanation>"}` |

**Password rules**: minimum 12 characters, maximum 128 characters. Rules beyond length are not required by this spec.

## POST /api/auth/invitations/accept

**Request**: `{"token": "<string>", "newPassword": "<string>"}`

| Status | When | Body |
|--------|------|------|
| `204 No Content` | Token valid; password set; account activated; email verified. | none |
| `400 Bad Request` | Token invalid, expired, or already used. | `{"message": "This invitation is no longer valid. Ask Caramel AI for a new one."}` |
| `400 Bad Request` | Password fails the password rules. | `{"message": "<rule explanation>"}` |

Invitations are created by the operator tool described in research R7; there is no public endpoint to create them.

## Cookie

- Name is server-defined and not part of the contract.
- `HttpOnly`, `Secure` outside localhost, `SameSite=Lax`.
- Not readable from JavaScript.

## Logging

Each sign-in, sign-out, lockout, reset request, and invitation acceptance writes one log line with the account ID (or a hash of the email) and the source IP. Passwords, tokens, cookies, and message content are never logged.
