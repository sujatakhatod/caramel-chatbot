# Feature Specification: Caramel AI Tech Support Chat (psec)

**Feature Branch**: `N/A (no git repository)`

**Created**: 2026-11-21

**Status**: Draft

**Input**: User description: "create psec using prd.md, governed by the project constitution"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ask a technical question and get a streamed answer (Priority: P1)

A developer integrating Caramel AI's inference API opens the chat, types a technical question
(for example, about a rate-limit error), and sees the answer appear progressively as it is written
rather than waiting for the full reply.

**Why this priority**: This is the core value of the product. Without it, no other story matters.

**Independent Test**: Open a fresh chat, send one on-topic question, and confirm that a streamed,
on-topic answer appears within the latency target. This alone delivers a usable MVP.

**Acceptance Scenarios**:

1. **Given** a fresh chat, **When** the user sends an on-topic question, **Then** a pending
   indicator appears immediately and the reply begins to appear within 3 seconds under normal
   conditions.
2. **Given** a reply is being generated, **When** the user watches the chat window, **Then** text
   appears progressively and the pending indicator clears when the reply completes.
3. **Given** a completed reply, **When** the user reads it, **Then** it is concise, friendly, and
   technical in tone.

---

### User Story 2 - Follow up within the same conversation (Priority: P2)

A user asks a follow-up question that refers to something said earlier in the same chat (for
example, "what about the enterprise tier?" after a billing question), and the assistant answers
in context.

**Why this priority**: Multi-turn context is what makes the assistant usable for real troubleshooting,
but a single-question exchange already delivers value.

**Independent Test**: Ask a question, then ask a follow-up that depends on the first answer, and
confirm the follow-up answer uses the earlier context.

**Acceptance Scenarios**:

1. **Given** the user asked about rate limits, **When** they send "and for the free tier?", **Then**
   the reply addresses rate limits for the free tier without requiring the question to be restated.
2. **Given** a conversation with several turns, **When** the user scrolls the message list, **Then**
   all earlier messages in this session remain visible.

---

### User Story 3 - Decline off-topic requests and recover from failures (Priority: P3)

A user asks something outside LLM inference support (for example, a general coding question or a
request for a poem), or the AI provider is slow or unavailable. The user gets a clear, polite
response and can continue using the chat.

**Why this priority**: Required for a safe customer-facing product, but it only matters once the
core question-and-answer path works.

**Independent Test**: Send an off-topic message and, separately, simulate a provider failure, then
confirm the user sees a polite decline or a friendly error and can send another message.

**Acceptance Scenarios**:

1. **Given** an off-topic message, **When** the user sends it, **Then** the assistant politely
   declines and points the user back to supported topics.
2. **Given** the AI provider times out, **When** the user sends a message, **Then** the user sees a
   friendly error message with no technical details and can send again.
3. **Given** the user asks about pricing or an SLA the assistant cannot verify, **When** the reply
   is shown, **Then** it contains no invented figures and states that the information is not
   known.

---

### User Story 4 - Start a new chat (Priority: P3)

A user who has finished one topic selects "New chat" to clear the window and start fresh.

**Why this priority**: A simple reset action that keeps conversations focused. Low risk, low effort.

**Independent Test**: Hold a multi-turn conversation, select "New chat", and confirm the window is
empty and the next question is answered without reference to the earlier conversation.

**Acceptance Scenarios**:

1. **Given** a conversation with prior messages, **When** the user selects "New chat", **Then** the
   message list is cleared.
2. **Given** a new chat has started, **When** the user asks a follow-up-style question, **Then** the
   assistant treats it as a first message with no prior context.

---

### Edge Cases

- A message exceeds the maximum allowed length: the message is not sent and the user is told the
  limit.
- A user sends many messages in quick succession: additional messages are held back with a friendly
  notice that the rate limit was reached, and the user can retry shortly after.
- The user sends an empty or whitespace-only message: nothing is sent.
- The user closes the browser tab and returns later: the previous conversation is no longer shown,
  since history does not persist across sessions.
- A reply stops partway through because of a failure: the partial text is kept visible and the
  user sees a friendly error note with the option to retry.
- A request comes from a web origin other than the official frontend: the request is refused.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to type a text message in a chat window and send it to the
  assistant.
- **FR-002**: The assistant's reply MUST be displayed progressively as it is generated, not shown
  only after completion.
- **FR-003**: The first visible part of a reply MUST appear within 3 seconds under normal
  conditions.
- **FR-004**: The chat window MUST show a visible pending indicator from the moment a message is
  sent until its reply completes or fails.
- **FR-005**: The assistant MUST keep conversation context for the duration of one chat session so
  that follow-up messages are answered in context.
- **FR-006**: Users MUST be able to start a new chat, which clears the visible history and the
  context used for replies.
- **FR-007**: The assistant MUST answer only questions about LLM inference API support: usage, model
  availability, rate limits, latency, errors, billing, and integration.
- **FR-008**: Off-topic messages MUST receive a polite decline that points the user back to
  supported topics.
- **FR-009**: The assistant MUST NOT state pricing, service-level commitments, model availability,
  or rate-limit figures that it cannot verify, and MUST say when it does not know an answer.
- **FR-010**: Replies MUST use a concise, friendly, and technical tone.
- **FR-011**: When the AI provider times out or is unavailable, the user MUST see a friendly error
  message that contains no technical details, and MUST be able to send another message.
- **FR-012**: Messages longer than 2,000 characters MUST NOT be sent, and the user MUST be told the
  limit.
- **FR-013**: The system MUST limit how many messages one session can send within a time window,
  and MUST show a friendly notice when the limit is reached.
- **FR-014**: Only the official frontend web origin MUST be allowed to use the chat service.
- **FR-015**: Provider credentials MUST NOT be visible to users, in the interface or in any
  response.
- **FR-016**: The system MUST record, for each request, its timing, outcome, and any error, and
  MUST NOT record the text of user messages or replies in these records.
- **FR-017**: Conversation history MUST NOT persist after the session ends. User accounts and
  sign-in are out of scope.
- **FR-018**: The MVP MUST NOT include live human handoff, ticket creation, or document-based
  answers from company documentation.

### Key Entities *(include if feature involves data)*

- **Chat Session**: One continuous conversation identified by a session identifier generated by
  the user's client. Holds an ordered list of messages and ends when the user starts a new chat or
  leaves the page.
- **Message**: A single entry in a session. Has a role (user or assistant), text content, and a
  time of creation.
- **Reply Status**: The state of an assistant reply: pending, streaming, complete, or failed.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In a test run under normal load, at least 95% of replies show their first visible
  text within 3 seconds of sending.
- **SC-002**: In a scripted set of off-topic prompts, 100% receive a polite decline and none receive
  an on-topic answer.
- **SC-003**: In a scripted set of pricing, SLA, and availability questions, 0 replies contain a
  figure the assistant could not verify.
- **SC-004**: In a scripted set of multi-turn conversations, at least 90% of follow-up answers
  correctly use context from earlier turns in the same session.
- **SC-005**: In simulated provider outages, 100% of affected users see a friendly message with no
  technical error text, and can send another message afterward.
- **SC-006**: After a user selects "New chat", 0 subsequent replies refer to conversation content
  from before the reset.
- **SC-007**: In a manual review of test sessions, no record of a request contains the text of a
  user message or reply.

## Assumptions

- The maximum message length is 2,000 characters, and the rate limit is set per session. The exact
  per-session limit will be chosen during planning and is not a product decision here.
- Target users are developers integrating the inference API and technical or billing contacts at
  customer companies, as described in the PRD.
- No escalation path is offered in the MVP. Where the assistant cannot resolve an issue, it says so
  and does not imply a human will follow up. The escalation path (PRD §10) is a deferred product
  decision.
- The assistant's knowledge is limited to its configured instructions and general knowledge; it
  does not have access to live company documentation (RAG is out of MVP scope).
- Users access the chat from a desktop or mobile web browser with a stable connection; native
  apps are out of scope.
- The choice of AI provider integration route is an implementation decision and is not specified
  here.
- Session identifiers are generated per browser session and are not tied to any user identity.
