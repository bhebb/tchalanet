# SALES / Approve Ticket — Backend (v1)

## ADDED Requirements

### SALES-APPROVE-001 — Approve endpoint

The system MUST expose `POST /api/v1/tenant/tickets/{ticket_id}/approve`.

#### Scenario: Approve pending ticket becomes SOLD

Given a ticket with `sale_status = PENDING_APPROVAL` and `settlement_status = UNSETTLED`
When an authorized actor calls the approve endpoint
Then the server responds `200`
And the ticket `sale_status = SOLD`

#### Scenario: Approving an already SOLD ticket is a no-op

Given a ticket with `sale_status = SOLD`
When an authorized actor calls the approve endpoint
Then the server responds `200`
And returns the current ticket summary

#### Scenario: Ticket not found

Given no ticket exists for the given id in tenant scope
When the actor calls the approve endpoint
Then the server responds `404`
And ProblemDetail.code is `sales.approve_ticket.not_found`

#### Scenario: State conflict (not pending)

Given a ticket exists but `sale_status = REJECTED`
When the actor calls the approve endpoint
Then the server responds `409`
And ProblemDetail.code is `sales.approve_ticket.state_conflict`

---

References:

- `openspec/context/05-version-guard.md`
- `openspec/context/10-non-negotiables.md`
- `openspec/context/20-backend-rules.md`
- `openspec/context/71-domain-sales.md`
- `openspec/context/26-ticket-codes.md`
- `openspec/context/PAGINATION.md` (reference only; not applicable here)
- `openspec/context/API_RESPONSE_STANDARDIZATION.md`

---

## Endpoint(s)

- POST `/api/v1/tenant/tickets/{ticket_id}/approve`

Scope:

- TENANT (authenticated)
- Tenant is resolved from request context; client MUST NOT provide tenant id.

---

## Purpose

Approve a ticket currently `PENDING_APPROVAL`. On success, the ticket transitions to `SOLD`.
Approval is **not** settlement and **not** resulting.

---

## Headers

### Required

- `Authorization: Bearer <token>`

### Optional

- `X-Request-Id`

Idempotency:

- NOT REQUIRED (idempotency is SELL-only).

---

## Request schema (normative)

### ApproveTicketRequest

- Path param: `ticket_id` (typed id)
- Body: none

Constraints:

- `tenant_id` MUST NOT be accepted (anywhere).
- Controller MUST accept typed wrapper ID for `ticket_id` (no raw UUID leaks outside persistence).

---

## Response schema (normative)

### ApproveTicketResponse

Returned as JSON in `ApiResponse<T>` envelope.

Fields:

- `ticket_id` (typed id)
- `ticket_code` (string; per-tenant unique; always present)
- `public_code` (string; globally unique; always present in MVP)
- `sale_status` (`SOLD` | `PENDING_APPROVAL` | `REJECTED` | `VOID`)
- `result_status` (`NOT_RESULTED` | `WON` | `LOST` | `OVERRIDDEN`)
- `settlement_status` (`UNSETTLED` | `SETTLED`)
- `currency` (string)
- `total_amount` (decimal, scale 2)
- `created_at` (instant)
- `updated_at` (instant)

Notes:

- On success, `sale_status` MUST be `SOLD`.
- `result_status` MUST NOT be changed by approval.
- `settlement_status` MUST NOT be changed by approval.
- `notices[]` MAY be returned through the standard `ApiResponse` notices mechanism.

---

## HTTP status mapping (normative)

- `200 OK` → Approved; ticket is now `SOLD`
- `404 Not Found` → Ticket not found in tenant scope
- `403 Forbidden` → Actor lacks permission to approve
- `409 Conflict` → State conflict (not pending, void, settled, etc.)
- `422 Unprocessable Entity` → Invalid request (reserved; rare)

---

## Business rules (normative)

### Preconditions (state + scope)

- Ticket MUST exist within tenant scope.
- `sale_status` MUST be `PENDING_APPROVAL`.
- Ticket MUST NOT be `VOID`.
- `settlement_status` MUST be `UNSETTLED` (approval does not settle; settled tickets cannot be approved).
- Approval MUST NOT change `result_status` (typically remains `NOT_RESULTED`).

### Decision table — Approval outcome

| Condition                                                                                                       | Result                                    |
| --------------------------------------------------------------------------------------------------------------- | ----------------------------------------- |
| Ticket exists in tenant AND `sale_status = PENDING_APPROVAL` AND not `VOID` AND `settlement_status = UNSETTLED` | Approve → `SOLD` (200)                    |
| Ticket missing in tenant                                                                                        | 404 `sales.approve_ticket.not_found`      |
| Actor lacks permission                                                                                          | 403 `sales.approve_ticket.forbidden`      |
| `sale_status != PENDING_APPROVAL`                                                                               | 409 `sales.approve_ticket.state_conflict` |
| Ticket is `VOID`                                                                                                | 409 `sales.approve_ticket.state_conflict` |
| Ticket is `SETTLED`                                                                                             | 409 `sales.approve_ticket.state_conflict` |
| Payload/validation anomaly (reserved)                                                                           | 422 `sales.approve_ticket.invalid`        |

### Repeat calls (normative behavior)

- If the ticket is already `SOLD`, the endpoint MUST return `200` with the current ticket summary (no additional state change).
- If the ticket is `REJECTED` or `VOID`, it MUST return `409 sales.approve_ticket.state_conflict`.

---

## Errors — ProblemDetail (canonical)

Error code convention:

- `sales.approve_ticket.<reason>`

ProblemDetail MUST follow RFC7807 + stable `code` field.

### 404 Not Found

- `sales.approve_ticket.not_found`

### 403 Forbidden

- `sales.approve_ticket.forbidden`

### 409 Conflict

- `sales.approve_ticket.state_conflict`

### 422 Unprocessable Entity

- `sales.approve_ticket.invalid`

Optional validation shape (if 422):

```json
{
  "violations": [{ "path": "", "code": "request.invalid", "message": "Invalid request" }]
}
```

---

## Events & audit

### Audit (normative)

On success (`200 OK`), the server MUST emit an audit entry:

- `action`: `approve_ticket`
- `actor`: from request context
- `resource`: `ticket_id`
- `timestamp`

### AfterCommit events (informative but expected)

`TicketApprovedEvent` with minimal payload:

- `tenant_id`
- `ticket_id`
- `total_amount`
- `currency`
- `occurred_at`

Notes:

- Publication MUST occur AfterCommit (post-transaction).

---

## Examples

### Example request

```http
POST /api/v1/tenant/tickets/tick_01J2Z8WQ4Q3W5D2Q9W7ZKJ6Q2B/approve
Authorization: Bearer <token>
X-Request-Id: 6f8d0a1e-3c4b-4f9e-bd2a-1b2c3d4e5f6a
```

### Example response (200)

```json
{
  "status": "SUCCESS",
  "data": {
    "ticket_id": "tick_01J2Z8WQ4Q3W5D2Q9W7ZKJ6Q2B",
    "ticket_code": "TCK-260113-214501-9K3W2H-7",
    "public_code": "0J3W6M1N7Q9A",
    "sale_status": "SOLD",
    "result_status": "NOT_RESULTED",
    "settlement_status": "UNSETTLED",
    "currency": "HTG",
    "total_amount": "35.00",
    "created_at": "2025-12-01T12:34:56Z",
    "updated_at": "2025-12-01T12:35:10Z"
  }
}
```
