# Data Model: User Authentication (002-user-auth)

Entities map to the spec's Key Entities. Tables are described by role; column types are chosen during implementation against both H2 and PostgreSQL.

## Account (`app_user`)

| Field | Rules |
|-------|-------|
| `id` | Unique, non-guessable identifier. |
| `email` | Required. Stored lowercased and trimmed. Unique. |
| `password_hash` | Required once the account is active. Never returned by any API. |
| `email_verified_at` | Set when the customer accepts an invitation or completes a reset. Chat requires it to be set (spec FR-011). |
| `failed_sign_in_count` | Integer, reset to 0 on successful sign-in. |
| `failed_sign_in_window_start` | Start of the current 15-minute counting window. |
| `locked_until` | Nullable. Sign-in refused while in the future (spec FR-009). |
| `created_at` | Set on creation. |

**Relationships**: owns zero or more chat sessions; has zero or more account tokens.

**State**: Invited (no password yet) → Active (password set, email verified) → Locked (temporary, `locked_until` in the future) → Active again when the lock ends.

## Account Token (`account_token`)

Used for invitations and password resets. Single use.

| Field | Rules |
|-------|-------|
| `id` | Unique. |
| `user_id` | Required. References Account. |
| `purpose` | One of `INVITATION`, `PASSWORD_RESET`. |
| `token_hash` | Required. SHA-256 of the random token. The raw token is never stored. |
| `expires_at` | Required. Reset: 30 minutes after creation. Invitation: 7 days after creation. |
| `used_at` | Nullable. Set on first use; any later use is refused. |

**Validation**: a token is valid only if `used_at` is null, `expires_at` is in the future, and `purpose` matches the operation.

## Chat Session Owner (`chat_session`)

| Field | Rules |
|-------|-------|
| `session_id` | Unique. Server-issued. Matches the session ID used by the in-process chat memory. |
| `user_id` | Required. References Account. Never changes. |
| `created_at` | Set on creation. |
| `last_activity_at` | Updated on each message. |

**Rule**: a request is allowed only when `chat_session.user_id` equals the signed-in user's ID (spec FR-007).

## Sign-in Session

Managed by Spring Session JDBC. Not designed as application entities. Relevant rules:

- Idle timeout 30 minutes; absolute maximum 12 hours (plan A4).
- Sign-out deletes the session row (spec FR-004).
- Locking an account deletes that account's sessions (so an attacker's existing session ends when a lock occurs).
