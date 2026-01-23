# OpenSpec — SALES / Sell Ticket — Terminal (POS) (v1)

- openspec/context/05-version-guard.md
- openspec/context/10-non-negotiables.md
- openspec/context/25-idempotency.md
- openspec/context/26-ticket-codes.md

## Goal

Define terminal/POS behavior for selling tickets under unstable connectivity:

- Minimal taps and fast entry
- Safe retries without duplicates (Idempotency-Key)
- Durable outbox for offline/timeout scenarios
- Print immediately after success (ESC/POS preferred)
- Provide PDF as optional share/archive

---

## Constraints (normative)

- Must work under flaky network (timeouts, intermittent connectivity)
- Must prevent duplicate sales on retries
- Must not block the UI for long operations
- Must keep sale intent durable until resolved

---

## Terminal sale flow (normative)

### Step 1 — Build draft

Terminal collects:

- draw_id
- lines[] (normalized input can be assisted locally but server canonicalizes)
- stake amounts
- bet_option when applicable

Terminal SHOULD validate basic format but MUST rely on server for final validation.

### Step 2 — Create sale intent + Idempotency-Key

On first submit:

- Terminal MUST create a persistent sale intent record in local storage
- Terminal MUST generate `Idempotency-Key` once and store it in the intent

### Step 3 — Submit (online)

Terminal sends:

- `POST /api/v1/tenant/tickets`
- Header `Idempotency-Key` REQUIRED
- Body = SellTicketRequest snapshot

### Step 4 — Resolve outcome

#### 201 SOLD

- Mark intent SUCCESS (store ticket_id, ticket_code, public_code)
- Immediately print via ESC/POS:
  - `GET /api/v1/tenant/tickets/{ticketId}/print.escpos`
- Offer secondary actions:
  - show public_code (QR/URL)
  - optionally download PDF

#### 202 PENDING_APPROVAL

- Mark intent SUCCESS_PENDING_APPROVAL
- Display “Approval required”
- DO NOT print as “final ticket” unless policy allows (tenant-specific; default: no final print)
- Optionally print a “pending slip” (future; non-goal in v1 unless required)

### Step 5 — Failure handling

- For transient failures (timeouts, 5xx, network lost):
  - Keep intent in outbox
  - Retry automatically (same Idempotency-Key)
- For validation errors:
  - Mark intent FAILED_VALIDATION
  - Show error and allow user to edit draft (new intent/key)

---

## Outbox (offline-first) — REQUIRED

### Local storage model (normative)

Each sale intent MUST persist:

- `local_intent_id`
- `created_at`
- `idempotency_key`
- `request_payload_snapshot`
- `status`:
  - `DRAFT`
  - `QUEUED`
  - `SENDING`
  - `RETRYING`
  - `SUCCESS_SOLD`
  - `SUCCESS_PENDING_APPROVAL`
  - `FAILED_VALIDATION`
  - `FAILED_BLOCKED`
  - `FAILED_FATAL`
- `retry_count`
- `last_attempt_at`
- `resolved_ticket_id` (when success)
- `ticket_code` / `public_code` (when success)
- `last_error_code` (ProblemDetail.code if available)

### Retry rules (normative)

- Terminal MUST retry `QUEUED/SENDING/RETRYING` intents with the same `Idempotency-Key`
- Terminal MUST use exponential backoff with jitter
- If terminal restarts, it MUST resume outbox processing
- If server returns `idempotency.in_progress`, terminal MUST retry (same key)

### When to create a new key (normative)

- Only if the user edits the draft payload (lines/stakes/draw/etc.)
- Changing any field means new sale intent and new key

---

## Printing behavior

### ESC/POS (preferred)

- After 201 SOLD, terminal SHOULD print using:
  - `GET /api/v1/tenant/tickets/{ticketId}/print.escpos`
- Terminal MUST treat print endpoint as non-cacheable (`no-store` on server)

### PDF (optional)

- Terminal MAY provide:
  - `GET /api/v1/tenant/tickets/{ticketId}/print.pdf`
- Use cases:
  - share via messaging
  - archive

### Reprint (v1)

- Terminal MAY allow reprint from ticket detail if the ticket is still printable
- Errors:
  - `409 ticket.state_conflict` when not printable

---

## Public verification support

- Terminal MUST display or embed `public_code` on printed receipts (QR or text)
- Terminal MAY display a “Verify” button that opens:
  - `/ticket/{publicCode}`

---

## Error mapping (normative)

### ticket.invalid (422)

- Mark intent `FAILED_VALIDATION`
- Show user the validation error
- Allow edit -> new intent/key

### draw.closed (409)

- Mark intent `FAILED_FATAL` (or dedicated `FAILED_CLOSED`)
- Prompt to select another draw

### limit.blocked (409)

- Mark intent `FAILED_BLOCKED`
- If tenant policy allows approval on block, server would have returned 202 instead
- Otherwise, show blocked message and stop

### idempotency.payload_mismatch (409)

- Mark intent `FAILED_FATAL`
- This indicates the stored intent diverged; require new intent

### idempotency.in_progress (409)

- Keep intent `RETRYING` and retry soon

---

## Minimal UX states (suggested)

- Draft entry
- Submitting (non-blocking spinner)
- Sold success + auto print
- Pending approval + reference shown
- Error states with clear actions

---

## Telemetry / audit (informative)

Terminal SHOULD log:

- local_intent_id
- idempotency_key
- request_id (if provided)
- resolved_ticket_id
  to help support and reconciliation.

---

## Non-goals (v1)

- Dedicated “pending approval slip” print format
- Offline ticket issuance without server persistence
- Batch sell
