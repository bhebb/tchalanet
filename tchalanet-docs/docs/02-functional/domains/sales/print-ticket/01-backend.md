# OpenSpec — SALES / Print Ticket — Backend (v1)

References:

- openspec/context/05-version-guard.md
- openspec/context/10-non-negotiables.md
- openspec/context/20-backend-rules.md
- openspec/context/71-domain-sales.md

---

## Endpoint(s)

- GET `/api/v1/tenant/tickets/{ticket_id}/print.pdf`

  - Produces: `application/pdf`
  - Headers:
    - `Cache-Control: no-store`

- GET `/api/v1/tenant/tickets/{ticket_id}/print.escpos`
  - Produces: `application/octet-stream`
  - Headers:
    - `Cache-Control: no-store`
    - `Content-Disposition: inline; filename="ticket-{ticket_code}.escpos"` (recommended)

---

## Purpose

Deliver printable representations of a ticket for authorized operators.

- PDF is intended for standard printing and sharing.
- ESC/POS is intended for POS printers.

Printing is a read-only operation and does not modify ticket state.

---

## Headers

### Required

- `Authorization` (tenant scope via auth/context)

### Optional

- `X-Request-Id`

### Caching

- Responses MUST include `Cache-Control: no-store`.

---

## Request schema (normative)

### PrintTicketRequest

- Path parameter:
  - `ticket_id` (typed id)
- No request body.

Constraints:

- `tenant_id` MUST NOT be accepted in the request body.
- Tenant scope is derived from auth/context.

---

## Response schema (normative)

### PDF

- Content-Type: `application/pdf`
- Body: binary PDF content

### ESC/POS

- Content-Type: `application/octet-stream`
- Body: binary ESC/POS byte stream
- Recommended header:
  - `Content-Disposition: inline; filename="ticket-{ticket_code}.escpos"`

---

## HTTP status mapping (normative)

| Status        | Meaning                                    |
| ------------- | ------------------------------------------ |
| 200 OK        | Printable content returned                 |
| 404 Not Found | Ticket not found in tenant scope           |
| 403 Forbidden | Actor lacks permission to print            |
| 409 Conflict  | Printing not allowed due to state conflict |

---

## Business rules (normative)

- Only authorized roles (cashier/operator/admin) MAY print tickets.
- Tickets in `VOID` state **MAY be printed**.
  - Printable content MUST clearly indicate `VOID` status
    (e.g., watermark, label, or header).
- Printable content MUST reflect the current ticket state:
  - `sale_status`
  - `result_status`
  - `settlement_status`
- Printable content MUST NOT expose sensitive PII.
  - Outlet information MAY be partially masked.

### Decision table — Print outcome

| Condition                                   | Result                                  |
| ------------------------------------------- | --------------------------------------- |
| Ticket exists in tenant AND actor permitted | 200 printable content                   |
| Ticket not found in tenant                  | 404 `sales.print_ticket.not_found`      |
| Actor lacks permission                      | 403 `sales.print_ticket.forbidden`      |
| Printing disallowed by policy               | 409 `sales.print_ticket.state_conflict` |

---

## Errors — ProblemDetail (canonical)

Error code convention:
`sales.print_ticket.<reason>`

ProblemDetail style:

- RFC7807 plus stable `code` field

### 404 Not Found

- `sales.print_ticket.not_found`

### 403 Forbidden

- `sales.print_ticket.forbidden`

### 409 Conflict

- `sales.print_ticket.state_conflict`

### 422 Unprocessable Entity (if applicable)

- `sales.print_ticket.invalid`

---

## Audit

### Audit (normative)

On each successful print (`200 OK`), the server MUST emit an audit entry:

- `action`: `print_ticket`
- `actor`: derived from auth/context
- `resource`: `ticket_id`
- `timestamp`

---

## Events

- No AfterCommit events are required for print delivery.

---

## Examples

### Example request — PDF

```http
GET /api/v1/tenant/tickets/tick_01J2Z8WQ4Q3W5D2Q9W7ZKJ6Q2B/print.pdf
Authorization: Bearer <token>
X-Request-Id: 6f8d0a1e-3c4b-4f9e-bd2a-1b2c3d4e5f6a
```

### Example response — PDF

```text
Content-Type: application/pdf
Cache-Control: no-store

<binary PDF content omitted>
```

### Example request — ESC/POS

```http
GET /api/v1/tenant/tickets/tick_01J2Z8WQ4Q3W5D2Q9W7ZKJ6Q2B/print.escpos
Authorization: Bearer <token>
X-Request-Id: 6f8d0a1e-3c4b-4f9e-bd2a-1b2c3d4e5f6a
```

### Example response — ESC/POS

```text
Content-Type: application/octet-stream
Content-Disposition: inline; filename="ticket-A1B2-C3D4-E5.escpos"
Cache-Control: no-store

<binary ESC/POS content omitted>
```

---

## Non-normative (debug-only) note

The following endpoint is debug-only and MUST NOT appear in the normative contract.
It MAY exist in development or behind feature flags and MAY be removed at any time:

```http
GET /api/v1/tenant/tickets/{ticket_id}/print
```

- Returns Base64-encoded PDF
- MUST NOT be used by UI/POS

---
