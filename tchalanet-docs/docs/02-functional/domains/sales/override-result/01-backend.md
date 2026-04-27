# OpenSpec — SALES / Override Result — Backend (v1)

References:

- openspec/context/05-version-guard.md
- openspec/context/10-non-negotiables.md
- openspec/context/20-backend-rules.md
- openspec/context/71-domain-sales.md

---

## Endpoint(s)

- `POST /api/v1/tenant/tickets/{ticket_id}/override-result`

---

## Purpose

Manually override the computed result of a ticket for exception handling.

On success:

- `result_status` is set to `OVERRIDDEN`
- an explicit override outcome (`WON` or `LOST`) is recorded in metadata

Override does **not** settle the ticket and does **not** trigger payout.

---

## Headers

### Required

- `Authorization` (tenant-scoped via auth/context)

### Optional

- `X-Request-Id`

### Idempotency

- **NOT REQUIRED**
- If provided, `Idempotency-Key` MUST be ignored.

---

## Request schema (normative)

### OverrideResultRequest

- `outcome` (enum: `WON` | `LOST`) — **required**
- `reason_code` (string, required; stable catalog of reasons)
- `reason_note` (string, optional; max length 500)

Constraints:

- `tenant_id` MUST NOT be accepted in request body.
- Actor identity is derived from auth/context.
- Server MUST set `result_status = OVERRIDDEN`; clients MUST NOT set it directly.

---

## Response schema (normative)

### OverrideResultResponse

- `ticket_id` (typed id)
- `ticket_code` (string; unique per tenant; always present)
- `public_code` (string; globally unique; always present in MVP)
- `sale_status` (`SOLD` | `PENDING_APPROVAL` | `REJECTED` | `VOID`)
- `result_status` (`OVERRIDDEN`)
- `override_outcome` (`WON` | `LOST`)
- `settlement_status` (`UNSETTLED`)
- `currency` (string)
- `total_amount` (decimal, scale 2)
- `override_metadata`:
  - `reason_code` (string)
  - `reason_note` (string, optional)
  - `actor_id` (typed id)
  - `occurred_at` (instant)
- `updated_at` (instant)

Normative note:

- On success, `settlement_status` MUST remain `UNSETTLED` under this policy.

---

## HTTP status mapping (normative)

| Status                   | Meaning                          |
| ------------------------ | -------------------------------- |
| 200 OK                   | Override applied successfully    |
| 404 Not Found            | Ticket not found in tenant scope |
| 403 Forbidden            | Actor lacks permission           |
| 409 Conflict             | Ticket state conflict            |
| 422 Unprocessable Entity | Invalid request payload          |

---

## Business rules (normative)

### Preconditions

- Ticket MUST exist in tenant scope.
- Ticket MUST NOT be `VOID`.
- Ticket MUST be `UNSETTLED`.
- Actor MUST be authorized to perform override.

### Result semantics (normative)

- Override sets `result_status = OVERRIDDEN`.
- Override outcome (`WON` or `LOST`) is stored separately as metadata.
- Override does **not** change `sale_status`.
- Override does **not** settle the ticket.

### Decision table

| Condition                                        | Result                                     |
| ------------------------------------------------ | ------------------------------------------ |
| Valid ticket, UNSETTLED, not VOID, valid payload | 200 → override applied                     |
| Ticket not found                                 | 404 `sales.override_result.not_found`      |
| Unauthorized actor                               | 403 `sales.override_result.forbidden`      |
| Ticket VOID                                      | 409 `sales.override_result.state_conflict` |
| Ticket SETTLED                                   | 409 `sales.override_result.state_conflict` |
| Invalid payload                                  | 422 `sales.override_result.invalid`        |

---

## Errors — ProblemDetail (canonical)

Error code convention:

- `sales.override_result.<reason>`

### 404 Not Found

- `sales.override_result.not_found`

### 403 Forbidden

- `sales.override_result.forbidden`

### 409 Conflict

- `sales.override_result.state_conflict`

### 422 Unprocessable Entity

- `sales.override_result.invalid`

#### Validation error shape (if 422)

```json
{
  "violations": [
    { "path": "reason_code", "code": "required", "message": "reason_code is required" },
    { "path": "outcome", "code": "enum", "message": "outcome must be WON or LOST" }
  ]
}
```

---

## Events & audit

### Audit (normative)

On success (200 OK), the server MUST emit an audit entry:

- `action`: `override_result`
- `actor`: derived from auth/context
- `resource`: `ticket_id`
- `reason_code`, `reason_note` (if provided)
- `timestamp`

### AfterCommit events (informative but expected)

`TicketResultOverriddenEvent` with minimal payload:

- `tenant_id`
- `ticket_id`
- `override_outcome`
- `occurred_at`

---

## Examples

### Example request

```
POST /api/v1/tenant/tickets/tick_01J2Z8WQ4Q3W5D2Q9W7ZKJ6Q2B/override-result
Authorization: Bearer <token>
X-Request-Id: 6f8d0a1e-3c4b-4f9e-bd2a-1b2c3d4e5f6a
Content-Type: application/json

{
  "outcome": "WON",
  "reason_code": "ADMIN_CORRECTION",
  "reason_note": "Manual correction due to official ruling"
}
```

### Example response (200 OK)

```json
{
  "ticket_id": "tick_01J2Z8WQ4Q3W5D2Q9W7ZKJ6Q2B",
  "ticket_code": "A1B2-C3D4-E5",
  "public_code": "PUB-9X2Z7Q4N1M",
  "sale_status": "SOLD",
  "result_status": "OVERRIDDEN",
  "override_outcome": "WON",
  "settlement_status": "UNSETTLED",
  "currency": "HTG",
  "total_amount": "35.00",
  "override_metadata": {
    "reason_code": "ADMIN_CORRECTION",
    "reason_note": "Manual correction due to official ruling",
    "actor_id": "user_01J2...",
    "occurred_at": "2026-01-18T10:15:00Z"
  },
  "updated_at": "2026-01-18T10:15:00Z"
}
```
