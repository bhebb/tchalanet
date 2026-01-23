# OpenSpec — SALES / Reject Ticket — Backend (v1)

References:

- openspec/context/05-version-guard.md
- openspec/context/10-non-negotiables.md
- openspec/context/20-backend-rules.md
- openspec/context/71-domain-sales.md

---

## Endpoint(s)

- POST /api/v1/tenant/tickets/{ticket_id}/reject

---

## Purpose

Reject a ticket that is currently `PENDING_APPROVAL`. On success, the ticket transitions to `REJECTED`. Rejection does not settle or result the ticket.

---

## Headers

### Required

- `Authorization` (tenant scope via auth)

### Optional

- `X-Request-Id`

Idempotency: NOT REQUIRED.

---

## Request schema (normative)

### RejectTicketRequest

- `reason_code` (string, required)
- `reason_note` (string, optional; max length 500)

Constraints:

- `tenant_id` MUST NOT be accepted in body. Tenant is derived from auth/context.
- Actor must have permission to reject within tenant scope.

---

## Response schema (normative)

### RejectTicketResponse

- `ticket_id` (typed id)
- `ticket_code` (string; unique per tenant; always present)
- `public_code` (string; globally unique; always present in MVP)
- `sale_status` (`REJECTED`)
- `result_status` (`NOT_RESULTED`)
- `settlement_status` (`UNSETTLED`)
- `currency` (string)
- `total_amount` (decimal, scale 2)
- `rejection_metadata` (object):
  - `reason_code` (string)
  - `reason_note` (string, optional)
  - `actor_id` (typed id)
  - `occurred_at` (instant)
- `updated_at` (instant)
- `notices[]` (optional warnings)

---

## HTTP status mapping (normative)

- `200 OK` → Ticket rejected; `sale_status = REJECTED`
- `404 Not Found` → Ticket not found in tenant scope
- `403 Forbidden` → Actor lacks permission to reject
- `409 Conflict` → State conflict (not pending / void / mismatched conditions)
- `422 Unprocessable Entity` → Invalid payload

---

## Business rules (normative)

### Preconditions

- Ticket MUST exist within tenant scope.
- `sale_status` MUST be `PENDING_APPROVAL`.
- Ticket MUST NOT be `VOID`.
- `settlement_status` MUST be `UNSETTLED`.
- `result_status` SHOULD remain `NOT_RESULTED`.

### Decision table — Rejection outcome

| Condition                                                                                              | Result                                   |
| ------------------------------------------------------------------------------------------------------ | ---------------------------------------- |
| Exists in tenant AND `sale_status = PENDING_APPROVAL` AND not `VOID` AND `UNSETTLED` AND valid payload | 200 reject → `REJECTED`                  |
| Missing in tenant                                                                                      | 404 `sales.reject_ticket.not_found`      |
| Actor lacks permission                                                                                 | 403 `sales.reject_ticket.forbidden`      |
| `sale_status != PENDING_APPROVAL`                                                                      | 409 `sales.reject_ticket.state_conflict` |
| Ticket is `VOID`                                                                                       | 409 `sales.reject_ticket.state_conflict` |
| Payload invalid                                                                                        | 422 `sales.reject_ticket.invalid`        |

---

## Errors — ProblemDetail (canonical)

Error code convention:

- `sales.reject_ticket.<reason>`

ProblemDetail style: RFC7807 plus stable `code` field.

### 404 Not Found

- `sales.reject_ticket.not_found`

### 403 Forbidden

- `sales.reject_ticket.forbidden`

### 409 Conflict

- `sales.reject_ticket.state_conflict`

### 422 Unprocessable Entity

- `sales.reject_ticket.invalid`

### Validation errors (if 422)

ProblemDetail MAY include:

```json
{
  "violations": [
    { "path": "reason_code", "code": "required", "message": "reason_code is required" },
    { "path": "reason_note", "code": "length", "message": "reason_note must be <= 500 characters" }
  ]
}
```

---

## Events & audit

### Audit (normative)

On success (`200`), the server MUST emit an audit entry:

- `action`: `reject_ticket`
- `actor`: derived from context
- `resource`: `ticket_id`
- `reason_code`, `reason_note` (if provided)
- `timestamp`

### AfterCommit events (informative but expected)

- `TicketRejectedEvent` — minimal payload:
  - `tenant_id`
  - `ticket_id`
  - `occurred_at`

---

## Examples

### Example request

```
POST /api/v1/tenant/tickets/tick_01J2Z8WQ.../reject HTTP/1.1
Authorization: Bearer <token>
X-Request-Id: 6f8d0a1e-3c4b-4f9e-bd2a-1b2c3d4e5f6a
Content-Type: application/json

{
  "reason_code": "LIMIT_EXCEEDED",
  "reason_note": "Exceeded risk threshold"
}
```

### Example response (200)

```json
{
  "ticket_id": "tick_01J2Z8WQ...",
  "ticket_code": "A1B2-C3D4-E5",
  "public_code": "PUB-9X2Z7Q4N1M",
  "sale_status": "REJECTED",
  "result_status": "NOT_RESULTED",
  "settlement_status": "UNSETTLED",
  "currency": "HTG",
  "total_amount": "35.00",
  "rejection_metadata": {
    "reason_code": "LIMIT_EXCEEDED",
    "reason_note": "Exceeded risk threshold",
    "actor_id": "user_01J2...",
    "occurred_at": "2026-01-18T10:20:00Z"
  },
  "updated_at": "2026-01-18T10:20:00Z"
}
```
