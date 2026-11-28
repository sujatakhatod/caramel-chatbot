# Feature Specification: User Authentication for the Caramel AI Tech Support Chat

**Feature Branch**: `N/A (no git repository)`

**Created**: 2026-11-21

**Status**: Draft

**Input**: User description: "create a new spec 002 inside @specs using @specs/002-prd-user-auth.md"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sign in before using the assistant (Priority: P1)

A Caramel AI customer opens the chat assistant and must sign in with their email address and password before they can ask a question. Once signed in, they can use the chat exactly as they do today.

**Why this priority**: Restricting access to known customers is the core reason for this feature. Without it, nothing else about accounts or per-user limits matters.

**Independent Test**: Open the chat while signed out, confirm that no question can be sent, sign in with a valid account, and confirm that a question can be sent and answered.

**Acceptance Scenarios**:

1. **Given** a visitor who is not signed in, **When** they open the chat, **Then** they see a sign-in screen and cannot send a message.
2. **Given** a customer with a verified account, **When** they enter the correct email and password, **Then** they reach the chat window and can send a question.
3. **Given** a visitor, **When** they enter an email and password that do not match an account, **Then** they see the message "Email or password is incorrect" and no hint about whether the email is registered.
4. **Given** a signed-in customer, **When** they choose sign out, **Then** the chat window closes, and they must sign in again to continue.

---

### User Story 2 - Keep each conversation tied to its owner (Priority: P1)

A signed-in customer's conversations belong to them. They can start new chats and continue their own chats, and they cannot see or continue another customer's conversation.

**Why this priority**: Without this, signing in would not protect customer conversations or make usage limits meaningful per customer. It is part of the minimum secure product.

**Independent Test**: Sign in as two different customers, start a chat as each, and confirm that neither can open or send messages into the other's chat.

**Acceptance Scenarios**:

1. **Given** a signed-in customer, **When** they select "New chat", **Then** a new conversation starts and is associated with their account.
2. **Given** two customers A and B, **When** A tries to continue a conversation that belongs to B, **Then** the attempt is refused and B's conversation is not shown to A.
3. **Given** a customer who signed out and signed back in, **When** they open the chat, **Then** they can continue their own conversation during the same session, if it is still available.

---

### User Story 3 - Stay signed in safely and recover access (Priority: P2)

A customer's sign-in ends after a period of inactivity or after a maximum time, and they are asked to sign in again without losing the message they were typing. A customer who forgets their password can reset it by email. Repeated failed sign-in attempts temporarily block sign-in for that account.

**Why this priority**: These protect accounts and let customers recover access on their own. The product can run without them for an early test group, but they are needed before wider release.

**Independent Test**: Let a signed-in session expire and confirm a sign-in prompt appears with the unsent draft preserved. Request a password reset and complete it. Enter the wrong password repeatedly and confirm sign-in is temporarily blocked.

**Acceptance Scenarios**:

1. **Given** a signed-in customer who has been inactive past the idle limit, **When** they send a message, **Then** they see a notice that their session has expired and a sign-in prompt, and the unsent text is still in the input box after they sign in.
2. **Given** a customer who forgot their password, **When** they request a reset for their email, **Then** they receive a reset link, and the link works once and expires after a short time.
3. **Given** an account with repeated failed sign-in attempts, **When** the limit is reached, **Then** sign-in for that account is blocked for a short period with a message that does not reveal whether the email is registered.
4. **Given** a locked account whose lock period has ended, **When** the customer signs in with the correct password, **Then** sign-in succeeds.

---

### Edge Cases

- What happens when a customer signs in on two devices at once? Both sessions remain valid until each expires or is signed out.
- What happens when a reset link is opened after it has been used or has expired? The customer sees a clear message and can request a new link.
- What happens when the email service is unavailable? Customers who already have a verified account can still sign in. Sign-up and password reset show a friendly message and can be retried later.
- What happens when a customer signs out while a reply is still streaming? The reply stops, and no further messages are sent under that account.
- What happens when a customer tries to sign up with an email that already has an account? The message does not confirm that the account exists.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The chat assistant MUST NOT accept questions from anyone who is not signed in.
- **FR-002**: Customers MUST be able to sign in with an email address and password.
- **FR-003**: Failed sign-in attempts MUST show the same generic message whether or not the email is registered.
- **FR-004**: Customers MUST be able to sign out, and signing out MUST end their access on that device.
- **FR-005**: Signed-in sessions MUST expire after a period of inactivity and after a maximum total duration. The idle and maximum durations are set during planning.
- **FR-006**: When a session expires, the customer MUST be prompted to sign in again, and any unsent text MUST be kept.
- **FR-007**: Every chat conversation MUST belong to the customer who created it. A customer MUST NOT be able to view or send messages into another customer's conversation.
- **FR-008**: Customers MUST be able to request a password reset link by email. Each reset link MUST work only once and expire after a short time (proposed: 30 minutes).
- **FR-009**: After repeated failed sign-in attempts on one account, sign-in for that account MUST be blocked for a limited time (proposed: 5 failures within 15 minutes, followed by a 15-minute block).
- **FR-010**: Usage limits MUST apply per customer, in addition to existing limits on request volume from a single source, which MUST also apply to sign-in and sign-up attempts.
- **FR-011**: Customers MUST be able to create an account with an email address and password, and MUST verify that email before they can use the chat assistant. [NEEDS CLARIFICATION: Can anyone create an account themselves, or are accounts created by Caramel AI and invited by email?]
- **FR-012**: Authentication events (sign-up, sign-in success and failure, sign-out, lockout, and password reset requests) MUST be recorded, without recording passwords, reset links, session credentials, or the content of any message.
- **FR-013**: Existing behavior of the assistant (its persona, its topic limits, and its streaming replies) MUST NOT change as a result of this feature.

### Key Entities

- **Customer account**: a person who can use the assistant. Has an email address, a verified status, and a sign-in lock state. Owns zero or more conversations.
- **Conversation**: a chat thread that belongs to exactly one customer account. Contains the messages exchanged in that thread.
- **Sign-in session**: a period during which a customer is signed in on one device. Ends by sign-out, inactivity, or the maximum duration.
- **Password reset request**: a single-use request, tied to one customer account, that expires after a short time.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of attempts to use the chat assistant without signing in are refused in acceptance testing.
- **SC-002**: In acceptance testing, no customer can view or send messages into another customer's conversation in any tested case.
- **SC-003**: 95% of customers with a correct password complete sign-in in under 10 seconds on their first attempt.
- **SC-004**: 90% of customers whose session expires keep their unsent message and can resume typing after signing in again.
- **SC-005**: A customer who forgets their password can complete a reset from the request to signed in in under 5 minutes.
- **SC-006**: Customers blocked by failed sign-in attempts can sign in again automatically once the block period ends, without contacting support.
- **SC-007**: No passwords, reset links, or message content appear in the recorded activity logs during acceptance review.

## Assumptions

- The assistant's existing behavior, including its topic limits and streaming replies, stays as it is in the MVP described in `prd.md`.
- Customers sign in with an email address and password. Single sign-on, multi-factor authentication, and team or organization accounts are out of scope for this feature.
- All customers share one default role. Admin tools are a separate feature.
- Conversations are kept only for the life of the server process, as in the MVP. Keeping them across sign-ins or restarts is a separate decision.
- Email is used for verification and password reset. An email provider and sending domain are needed before release.
- Idle and maximum session durations, and the lockout thresholds, use the proposed values in this spec until planning confirms them.
- The source requirements are in [prd-user-auth](../002-prd-user-auth.md).
