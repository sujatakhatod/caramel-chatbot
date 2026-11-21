# Quickstart: Validate Caramel AI Tech Support Chat (001-psec)

**Purpose**: Prove the feature works end to end. Implementation details live in `plan.md`,
`data-model.md`, and `contracts/chat-api.md`.

## Prerequisites

- A Google AI Studio API key with access to `gemma-4-26b-a4b-it`.
- A root `.env` file created from `.env.example`, containing `GOOGLE_API_KEY=<your key>`.
- Run and build commands are added to `CLAUDE.md` when scaffolding (PRD milestone 1) lands.

## 1. Confirm the model route (gate for PRD milestone 2)

Make one authenticated request to `gemma-4-26b-a4b-it` through the chosen route.

- **Expected**: A normal completion.
- **If rejected** ("model not found" or similar): stop and escalate. Do not substitute a model
  silently. See `research.md` R1.

## 2. Backend contract smoke test

With the backend running on its configured local port:

```bash
curl -N -X POST http://localhost:<port>/api/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"qs-1","message":"What rate limits apply to the free tier?"}'
```

**Expected**: A `text/event-stream` response with `token` events, ending in one `done` event.
The first `token` arrives within 3 seconds under normal conditions (spec SC-001).

## 3. Rejection checks

```bash
# Blank message: expect 400 with JSON error
curl -i -X POST http://localhost:<port>/api/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"qs-1","message":"   "}'
```

Repeat with a 2,001-character message and expect `413`.

## 4. Scenario checks (MVP acceptance, spec User Stories 1 to 4)

| # | Scenario | Expected outcome |
|---|----------|------------------|
| 1 | Ask an on-topic question in a new chat | Streamed, on-topic answer with a pending indicator that clears on completion |
| 2 | Ask "and for the enterprise tier?" after a rate-limit question | Answer uses the earlier context |
| 3 | Ask an off-topic question (for example, a poem) | Polite decline pointing back to supported topics |
| 4 | Ask for pricing or an SLA the assistant cannot verify | No invented figures; the assistant says it does not know |
| 5 | Simulate a provider timeout | Friendly error message, no technical text; user can send again |
| 6 | Select "New chat", then ask a follow-up | No reference to the earlier conversation |
| 7 | Review backend logs after scenarios 1 to 6 | Session ID, latency, and outcome present; no message text |

## 5. Security spot checks

- Inspect the browser network traffic and page source: the Google API key must not appear.
- Send a request with an unlisted `Origin` header: expect rejection (spec FR-014).

## Done when

All scenarios in section 4 pass, and the section 2 and 3 checks match the contract.
