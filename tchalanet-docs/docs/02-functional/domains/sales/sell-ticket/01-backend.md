# OpenSpec — SALES / Sell Ticket — Backend (v1)

References:

- openspec/context/05-version-guard.md
- openspec/context/10-non-negotiables.md
- openspec/context/25-idempotency.md
- openspec/context/26-ticket-codes.md

---

## Endpoint

- `POST /api/v1/tenant/tickets`

---

## Purpose

Create a ticket with one or more bet lines for a given draw and terminal session.

The server returns a persisted ticket in one of the following states:

- `SOLD` (HTTP 201), or
- `PENDING_APPROVAL` (HTTP 202)

This endpoint is idempotent and represents a single sale intent.

---

## Headers

### Required

- `Idempotency-Key` (REQUIRED)

### Optional

- `X-Request-Id`

---

## Request schema

### SellTicketRequest (normative)

- `draw_id` (typed id, required)
- `terminal_id` (typed id, required)
- `currency` (string, optional; defaults to tenant currency)
- `lines` (array, required, min 1)
  - `game_code` (string, required)
  - `bet_type` (string enum, required)
  - `bet_option` (integer/short, optional; required only for option-based bet types)
  - `selection` (string, required)
  - `stake` (decimal, required, > 0, scale 2)

### Request constraints

- `tenant_id` MUST NOT be accepted in the request body (derived from context).
- Server MUST validate that there is an open POS session for `(tenant, terminal_id)`.

---

## Line normalization (normative)

The server MUST canonicalize each `selection` before validation and persistence.

Rules (non-exhaustive):

- Numeric selections MUST be left-padded to expected length
  - `"1"` → `"01"` (2D), `"7"` → `"007"` (3D)
- Pair/triple selections MAY contain separators and MUST normalize to `-`
  - `"12 34"` → `"12-34"`
- Pattern selections MAY contain digits and `*` and MUST match expected length
  - e.g. `"****"` for 4-digit patterns

> Exact per-bet validation rules belong to domain logic. This spec mandates the existence and application of canonicalization.

---

## Merge duplicate lines (normative)

The server MUST merge duplicate bet lines using the canonical key:

`(game_code, bet_type, bet_option, selection_normalized)`

- `stake` is summed for identical keys
- merged result is used for:
  - persistence
  - total calculation
  - limit evaluation

### Determinism guarantee

After canonicalization and merge, the ticket total MUST be deterministic.

---

## Response schema

### SellTicketResponse (normative)

- `ticket_id` (typed id)
- `ticket_code` (string; unique per tenant; always present)
- `public_code` (string; globally unique; always present in MVP)
- `sale_status` (`SOLD` | `PENDING_APPROVAL`)
- `result_status` (`NOT_RESULTED`)
- `settlement_status` (`UNSETTLED`)
- `currency` (string)
- `total_amount` (decimal, scale 2)
- `created_at` (instant)
- `approval_request_id` (uuid, nullable; present when `PENDING_APPROVAL`)
- `notices[]` (optional warnings via ApiResponse.notices)

---

## HTTP status mapping (normative)

- `201 Created`
  - Ticket created with `sale_status = SOLD`
- `202 Accepted`
  - Ticket created with `sale_status = PENDING_APPROVAL`

---

## Idempotency — SELL (REQUIRED)

See: `openspec/context/25-idempotency.md`

### Normative behavior

- Missing key → `400` `sales.sell.idempotency.missing`
- Same key + different payload → `409` `sales.sell.idempotency.payload_mismatch`
- Same key while request in progress → `409` `sales.sell.idempotency.in_progress`
- Same key + same payload + already completed → server MUST replay the same result (same `ticket_id`, same status)

---

## Business rules

### Session validation (normative)

- Server MUST require an open POS session for `(tenant, terminal_id)`
- If missing: `409` `sales.sell.session.required`

### Draw cutoff (normative)

- Server MUST reject sell if current time is after draw cutoff
- Error: `409` `sales.sell.draw.closed`

### Limits & autonomy decision table (normative)

| Limit outcome | Autonomy allows approval | Result                                  |
| ------------- | -----------------------: | --------------------------------------- |
| OK / WARN     |                      n/a | Ticket created `SOLD` (201)             |
| BLOCK         |                      YES | Ticket created `PENDING_APPROVAL` (202) |
| BLOCK         |                       NO | `409` `sales.sell.limit.blocked`        |

### Warnings (non-blocking)

- WARN does NOT block the sale
- Ticket is created `SOLD`
- Warnings MAY be returned in `notices[]`

---

## Errors — ProblemDetail (canonical)

### Error code convention

`sales.<feature>.<reason>`

### 400

- `sales.sell.idempotency.missing`

### 409

- `sales.sell.idempotency.payload_mismatch`
- `sales.sell.idempotency.in_progress`
- `sales.sell.draw.closed`
- `sales.sell.limit.blocked`
- `sales.sell.session.required`
- `sales.sell.ticket.state_conflict` (reserved)

### 422

- `sales.sell.ticket.invalid`

#### Validation error shape (normative)

ProblemDetail MAY include:

```json
{
  "violations": [
    {
      "path": "lines[0].selection",
      "code": "selection.invalid",
      "message": "Selection does not match expected format"
    }
  ]
}
```

---

## Events & audit

### Audit (normative)

On success (201 or 202), the server MUST emit an audit entry:

- `action`: `sell_ticket`
- `actor`: derived from context (user / terminal / session)
- `resource`: `ticket_id`
- `timestamp`

### AfterCommit events (informative but expected)

- `TicketSoldEvent` (when SOLD)
- `TicketPendingApprovalEvent` (when PENDING_APPROVAL)

Payload (high-level):

- `tenant_id`
- `ticket_id`
- `draw_id`
- `terminal_id`
- `total_amount`
- `currency`
- `occurred_at`

---

## Example

### Request

```json
{
  "draw_id": "draw_01J2Z8WQ4Q3W5D2Q9W7ZKJ6Q2B",
  "terminal_id": "term_01J2Z8YF9N7F0E1S2V8C9M3L1A",
  "currency": "HTG",
  "lines": [
    {
      "game_code": "HT_BOLET",
      "bet_type": "MATCH_1_2D",
      "bet_option": null,
      "selection": "1",
      "stake": "25.00"
    },
    {
      "game_code": "HT_BOLET",
      "bet_type": "MATCH_1_2D",
      "bet_option": null,
      "selection": "01",
      "stake": "10.00"
    }
  ]
}
```

```

```
