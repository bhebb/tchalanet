# OpenSpec — SALES / Void Ticket — Backend (v1)

References:

- openspec/context/05-version-guard.md
- openspec/context/10-non-negotiables.md
- openspec/context/20-backend-rules.md
- openspec/context/71-domain-sales.md

---

## Endpoint(s)

- POST /api/v1/tenant/tickets/{ticket_id}/void

---

## Purpose

Void (cancel) a ticket. On success, `sale_status` becomes `VOID`. In v1, void is not allowed if `settlement_status = SETTLED`. A configurable void window applies.

---

## Headers

### Required

- `Authorization` (tenant scope via auth)

### Optional

- `X-Request-Id`

Idempotency: NOT REQUIRED.

---

## Request schema (normative)

### VoidTicketRequest

- `reason_code` (string, required)
- `reason_note` (string, optional; max length 500)

Constraints:

- `tenant_id` MUST NOT be accepted in body. Tenant is derived from auth/context.
- Actor must have permission to void within tenant scope.

---

## Response schema (normative)

### VoidTicketResponse

- `ticket_id` (typed id)
- `ticket_code` (string; unique per tenant; always present)
- `public_code` (string; globally unique; always present in MVP)
- `sale_status` (`VOID`)
- `result_status` (`NOT_RESULTED` | `WON` | `LOST` | `OVERRIDDEN`) — unchanged by void
- `settlement_status` (`UNSETTLED` | `SETTLED`)
- `currency` (string)
- `total_amount` (decimal, scale 2)
- `void_metadata` (object):
  - `reason_code` (string)
  - `reason_note` (string, optional)
  - `actor_id` (typed id)
  - `occurred_at` (instant)
- `updated_at` (instant)
- `notices[]` (optional warnings)

---

## HTTP status mapping (normative)

- `200 OK` → Ticket voided; `sale_status = VOID`
- `404 Not Found` → Ticket not found in tenant scope
- `403 Forbidden` → Actor lacks permission to void
- `409 Conflict` → State conflict (`settlement_status = SETTLED` in v1; already `VOID`; or window expired)
- `422 Unprocessable Entity` → Invalid payload

---

## Business rules (normative)

### Preconditions

- Ticket MUST exist within tenant scope.
- `settlement_status` MUST be `UNSETTLED`.
- `sale_status` MUST NOT already be `VOID`.
- Must be within the configurable void window:
  - `now <= ticket.created_at + void_window`
  - `void_window` comes from app settings: outlet override if present, else tenant default.

### Decision table — Void outcome

| Condition                                                                                        | Result                                 |
| ------------------------------------------------------------------------------------------------ | -------------------------------------- |
| Exists in tenant AND `UNSETTLED` AND not already `VOID` AND within void window AND valid payload | 200 void → `VOID`                      |
| Missing in tenant                                                                                | 404 `sales.void_ticket.not_found`      |
| Actor lacks permission                                                                           | 403 `sales.void_ticket.forbidden`      |
| `settlement_status = SETTLED`                                                                    | 409 `sales.void_ticket.state_conflict` |
| Already `sale_status = VOID`                                                                     | 409 `sales.void_ticket.state_conflict` |
| Outside void window                                                                              | 409 `sales.void_ticket.window_expired` |
| Payload invalid                                                                                  | 422 `sales.void_ticket.invalid`        |

---

## Errors — ProblemDetail (canonical)

Error code convention:

- `sales.void_ticket.<reason>`

ProblemDetail style: RFC7807 plus stable `code` field.

### 404 Not Found

- `sales.void_ticket.not_found`

### 403 Forbidden

- `sales.void_ticket.forbidden`

### 409 Conflict

- `sales.void_ticket.state_conflict`
- `sales.void_ticket.window_expired`

### 422 Unprocessable Entity

- `sales.void_ticket.invalid`

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

- `action`: `void_ticket`
- `actor`: derived from context
- `resource`: `ticket_id`
- `reason_code`, `reason_note` (if provided)
- `timestamp`

### AfterCommit events (informative but expected)

- `TicketVoidedEvent` — minimal payload:
  - `tenant_id`
  - `ticket_id`
  - `occurred_at`

---

## Examples

### Example request

```
POST /api/v1/tenant/tickets/tick_01J2Z8WQ.../void HTTP/1.1
Authorization: Bearer <token>
X-Request-Id: 6f8d0a1e-3c4b-4f9e-bd2a-1b2c3d4e5f6a
Content-Type: application/json

{
  "reason_code": "CUSTOMER_REQUEST",
  "reason_note": "Customer asked to cancel immediately"
}
```

### Example response (200)

```json
{
  "ticket_id": "tick_01J2Z8WQ...",
  "ticket_code": "A1B2-C3D4-E5",
  "public_code": "PUB-9X2Z7Q4N1M",
  "sale_status": "VOID",
  "result_status": "NOT_RESULTED",
  "settlement_status": "UNSETTLED",
  "currency": "HTG",
  "total_amount": "35.00",
  "void_metadata": {
    "reason_code": "CUSTOMER_REQUEST",
    "reason_note": "Customer asked to cancel immediately",
    "actor_id": "user_01J2...",
    "occurred_at": "2026-01-18T10:25:00Z"
  },
  "updated_at": "2026-01-18T10:25:00Z"
}
```

### Example error (409 window expired)

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Void window expired",
  "code": "sales.void_ticket.window_expired"
}
```
