# Contract Changes: Chat API (from 001-psec)

**Date**: 2026-11-28
**Base contract**: [../../001-psec/contracts/chat-api.md](../../001-psec/contracts/chat-api.md)

This file lists only what changes. Everything not listed keeps the 001-psec behavior, including the SSE event shapes.

## New: POST /api/chat/sessions

Creates a chat session owned by the signed-in user.

| Status | When | Body |
|--------|------|------|
| `201 Created` | Signed in. | `{"sessionId": "<server-issued string>"}` |
| `401 Unauthorized` | No valid session. | `{"message": "Please sign in."}` |

Clients call this for "New chat" (spec FR-010 of the user story) and do not generate session IDs themselves.

## Changed: POST /api/chat

**Request** (changed):
- `sessionId` must be a session created by `POST /api/chat/sessions` for the signed-in user. Client-generated IDs are refused.

**New rejections** (before streaming, returned as JSON):

| Status | When | Body |
|--------|------|------|
| `401 Unauthorized` | No valid session cookie. | `{"message": "Please sign in."}` |
| `403 Forbidden` | Session belongs to another user, or the session does not exist. | `{"message": "This chat is not available."}` |

**Rule**: the `403` message is the same for "not yours" and "does not exist", so an attacker cannot probe for session IDs.

**Rate limits** (changed): the existing per-session limit (20 requests per minute, from `001-psec` O4) applies per signed-in user. A per-IP limit also applies. Rejections keep the `429` shape from 001-psec.

**Streaming events**: unchanged (`token`, `done`, `error`). The `error` event still carries user-safe text only.

**Cross-origin requests**: CORS keeps the frontend origin and allows credentials. A wildcard origin is not permitted.
