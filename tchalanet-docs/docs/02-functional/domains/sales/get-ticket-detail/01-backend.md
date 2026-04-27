# OpenSpec — SALES / Get Ticket Detail — Backend (v1)

References:

- openspec/context/05-version-guard.md
- openspec/context/10-non-negotiables.md
- openspec/context/20-backend-rules.md
- openspec/context/71-domain-sales.md
- openspec/context/26-ticket-codes.md

---

## Endpoint(s)

Tenant:

- GET /api/v1/tenant/tickets/{ticket_id}

Public (verify scope; canonical):

- GET /api/v1/public/tickets/verify/{publicCode}

---

## Purpose

Return the complete detail for a ticket within tenant scope (by `ticket_id`).
Public access returns a masked subset via the **public verify** API (by `publicCode`).

---

## Headers

Tenant endpoint:

- `Authorization` (required)
- `X-Request-Id` (optional)

Public verify:

- unauthenticated
- MUST be rate-limited and noindex (see public-verify-ticket spec)

---

## Request schema (normative)

Tenant:

- Path: `ticket_id` (typed id)

Public:

- Path: `publicCode` (string; non-guessable)

Constraints:

- `tenant_id` MUST NOT be accepted in body/query. Tenant is derived from auth/context.
- Public responses MUST be masked (no sensitive PII; outlet partial/masked).

---

## Response schema (normative)

### Success envelope

- Tenant success MUST return `ApiResponse<TicketDetailResponse>`
- Public success MUST return `ApiResponse<TicketPublicView>` (as defined by public verify spec)

### TicketDetailResponse (tenant)

Fields (semantic requirements):

- `ticket_id` (typed id)
- `ticket_code` (string; unique per tenant; always present)
- `public_code` (string; always present in MVP)
- `sale_status` (`SOLD` | `PENDING_APPROVAL` | `REJECTED` | `VOID`)
- `result_status` (`NOT_RESULTED` | `WON` | `LOST` | `OVERRIDDEN`)
- `settlement_status` (`UNSETTLED` | `SETTLED`)
- `currency`
- `total_amount` (decimal scale 2)
- `created_at` (instant)
- `updated_at` (instant)
- `lines[]` (array) — each line includes:
  - `line_id` (typed id)
  - `selection` (string)
  - `odds` (decimal, optional by game)
  - `stake_amount` (decimal)
  - `result_status` (`NOT_RESULTED` | `WON` | `LOST` | `OVERRIDDEN`)
- `settlement` (optional) — only if settled
- `notices[]` (optional warnings in ApiResponse)

Public response is defined by: `openspec/specs/sales/public-verify-ticket/*`.

---

## HTTP status mapping (normative)

Tenant endpoint:

- `200 OK` → `ApiResponse<TicketDetailResponse>`
- `404 Not Found` → `ProblemDetail` code `ticket.not_found`
- `403 Forbidden` → `ProblemDetail` code `auth.forbidden` (optional later, if you enable it)

Public verify endpoint (single):

- `200 OK` → `ApiResponse<TicketPublicView>`
- `404 Not Found` → `ProblemDetail` code `ticket.not_found`
- `410 Gone` → `ProblemDetail` code `ticket.expired`
- `429 Too Many Requests` → `ProblemDetail` code `rate_limited` (optional later; keep consistent with index)

---

## Errors — ProblemDetail (canonical)

This spec MUST reuse canonical codes from `SALES Index (ACTED)`.

Tenant:

- `ticket.not_found`
- `ticket.state_conflict` (only if your domain allows “detail blocked by state”; otherwise omit)
- _(optional later)_ `auth.forbidden`, `auth.unauthorized`

Public:

- `ticket.not_found`
- `ticket.expired`
- _(optional later)_ `rate_limited`

---

## Examples

### Example request (tenant)

GET /api/v1/tenant/tickets/tick_01J2Z8WQ4Q3W5D2Q9W7ZKJ6Q2B
Authorization: Bearer <token>
X-Request-Id: 6f8d0a1e-3c4b-4f9e-bd2a-1b2c3d4e5f6a

### Example response (tenant 200)

```json
{
  "data": {
    "ticket_id": "tick_01J2Z8WQ4Q3W5D2Q9W7ZKJ6Q2B",
    "ticket_code": "TCK-260113-214501-9K3W2H-7",
    "public_code": "9Q2H7M4K1PZX",
    "sale_status": "SOLD",
    "result_status": "NOT_RESULTED",
    "settlement_status": "UNSETTLED",
    "currency": "HTG",
    "total_amount": "35.00",
    "created_at": "2025-12-01T12:34:56Z",
    "updated_at": "2025-12-01T12:35:10Z",
    "lines": [
      {
        "line_id": "line_01J2Z8WQ4QA...",
        "selection": "Team A vs Team B — WIN A",
        "odds": "1.80",
        "stake_amount": "20.00",
        "result_status": "NOT_RESULTED"
      }
    ]
  }
}
```
