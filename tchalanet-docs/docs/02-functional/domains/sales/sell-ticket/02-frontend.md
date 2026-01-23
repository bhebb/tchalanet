# OpenSpec — SALES / Sell Ticket — Frontend (Web) (v1)

- openspec/context/05-version-guard.md
- openspec/context/10-non-negotiables.md
- openspec/context/25-idempotency.md
- openspec/context/26-ticket-codes.md

## Goal

Define the web UX contract for selling a ticket:

- Build a ticket draft (draw + lines)
- Submit sale to backend with required headers
- Handle outcomes: SOLD vs PENDING_APPROVAL
- Handle retries safely with Idempotency-Key
- Provide access to print/download flows (ESC/POS not required on web)

This document defines UX states, API mapping, i18n keys, and accessibility expectations.

---

## Primary routes (suggested)

- `/app/sales/sell` (main sell flow)
- `/app/tickets` (list)
- `/app/tickets/:ticketId` (detail)
- `/ticket/:publicCode` (public verify; outside tenant area)

> Routes are informative; backend contract remains the source of truth.

---

## UX flow (normative)

### Step 1 — Draft

User selects:

- `draw`
- `lines[]` (one or more):
  - game
  - bet_type
  - bet_option (when applicable)
  - selection
  - stake

Draft rules:

- At least one line is required to submit
- Stakes must be positive
- Client MAY offer inline validation, but server remains the authority

### Step 2 — Submit

On submit, client:

1. Creates a single **sale intent** record (draft snapshot)
2. Generates `Idempotency-Key` once for this intent
3. Calls `POST /api/v1/tenant/tickets` with header `Idempotency-Key`

### Step 3 — Result

Client displays one of two success states:

#### Success: SOLD (201)

Show:

- ticket_code
- created_at
- total_amount + currency
- action buttons:
  - View ticket detail
  - Download PDF
  - Copy public verification link (public_code)
  - Share (optional)

#### Success: PENDING_APPROVAL (202)

Show:

- “Pending approval” banner
- ticket_code + approval_request_id
- Next actions:
  - View ticket detail
  - Copy reference for operator
  - (Optional) link to approval requests screen (future)

### Step 4 — Post-success navigation

- Client SHOULD redirect to ticket detail after success
- Client MUST preserve “success summary” (ticket_code) for at least the current session

---

## Idempotency (client-side behavior) — REQUIRED

See `openspec/context/25-idempotency.md`.

### Rules (normative)

- Client MUST generate one `Idempotency-Key` **per sale intent**
- Client MUST reuse the same key for all retries of that intent
- Client MUST NOT generate a new key on network timeout/retry
- If user edits the draft after a failed attempt, client MUST generate a new key
- Client SHOULD persist the intent record locally until resolved (success or user cancels)

### Recommended local intent model (informative)

- `sale_intent_id` (local uuid)
- `idempotency_key`
- `request_payload_snapshot`
- `request_created_at`
- `status`: `DRAFT | SUBMITTING | RETRYING | SUCCESS | FAILED`
- `resolved_ticket_id` (optional)
- `last_error_code` (optional)

---

## API mapping

### Sell

- Request:
  - `POST /api/v1/tenant/tickets`
  - Headers:
    - `Idempotency-Key` REQUIRED
    - `X-Request-Id` optional
  - Body:
    - `SellTicketRequest` (from backend spec)
- Success:
  - `201` -> SOLD
  - `202` -> PENDING_APPROVAL

### Print / PDF

- Web uses PDF:
  - `GET /api/v1/tenant/tickets/{ticketId}/print.pdf`
- Headers:
  - client should not cache (browser respects `Cache-Control: no-store`)
- UI:
  - open in new tab or download

### Public verify link (copy/share)

- Build link from `public_code`:
  - `/ticket/{publicCode}` (public route)
- Client MUST NOT expose tenant-scoped identifiers in public links

---

## Error handling (normative)

### Canonical mapping (ProblemDetail.code)

Client MUST map errors to user-facing messaging:

- `ticket.invalid` (422)
  - Show inline validation summary
  - Highlight line(s) if possible
- `draw.closed` (409)
  - Inform draw is closed/cutoff passed
  - Suggest selecting another draw
- `limit.blocked` (409)
  - Show “Sale blocked by policy”
  - Display details if provided (safe subset)
- `idempotency.in_progress` (409)
  - Show “Request in progress, retrying…”
  - Client SHOULD retry with exponential backoff
- `idempotency.payload_mismatch` (409)
  - Show “This sale attempt changed; please resubmit”
  - Force user to create a new intent
- `idempotency.missing` (400)
  - Treat as client bug
  - Show generic “technical error” + log telemetry

### Retry policy (informative)

- For transient failures (network, 502/503, timeouts):
  - Retry same request with same `Idempotency-Key`
  - Backoff: 0.5s, 1s, 2s, 5s (cap)
- For `idempotency.in_progress`:
  - Retry after short delay (same key)
- For `payload_mismatch`:
  - Do NOT retry; require new key + resubmit

---

## UI states (suggested)

- `draft`
- `submitting` (disable submit button)
- `success_sold`
- `success_pending_approval`
- `error_validation`
- `error_blocked`
- `error_closed`
- `error_transient_retrying`

---

## i18n keys (proposal; snake_case + namespaces)

### Screens

- `sales.sell.title`
- `sales.sell.subtitle`
- `sales.sell.lines_title`
- `sales.sell.add_line`
- `sales.sell.submit`

### Success

- `sales.sell.success_sold_title`
- `sales.sell.success_pending_title`
- `sales.sell.ticket_code_label`
- `sales.sell.public_link_label`
- `sales.sell.download_pdf`
- `sales.sell.view_ticket`

### Errors

- `errors.ticket_invalid`
- `errors.draw_closed`
- `errors.limit_blocked`
- `errors.idempotency_in_progress`
- `errors.idempotency_payload_mismatch`
- `errors.technical`

### Notices

- `notices.approval_required`
- `notices.limit_warn`

---

## Accessibility (normative)

- Submit button MUST have clear loading state and aria-busy
- Validation errors MUST be announced (aria-live)
- “Pending approval” state MUST be visually distinct and accessible
- PDF download link MUST include accessible label

---

## Non-goals

- This document does not define admin approval UI (separate feature).
- This document does not define offline mode for web (terminal handles offline).
