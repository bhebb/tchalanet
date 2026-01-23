# OpenSpec — SALES / List Tickets — Backend (v1) (ACTED-ready)

References:

- openspec/context/05-version-guard.md
- openspec/context/10-non-negotiables.md
- openspec/context/20-backend-rules.md
- openspec/context/71-domain-sales.md
- openspec/context/26-ticket-codes.md

---

## Endpoint(s)

- `GET /api/v1/tenant/tickets`

---

## Purpose

List tickets within tenant scope using **offset-based pagination** and filters.
Returns **ticket summaries only** (no lines).

---

## Headers

### Required

- `Authorization` (tenant scope via auth/context)

### Optional

- `X-Request-Id`

---

## Request schema (normative)

### ListTicketsRequest — query parameters

#### Pagination (offset-based; project standard `@TchPaging`)

- `page` (integer ≥ 0)
  - default: `@TchPaging.defaultPage` (currently 0)
- `size` (integer 1..maxSize)
  - default: `@TchPaging.defaultSize` (currently 20)
  - max: `@TchPaging.maxSize` (currently 100)
- `sort` (repeatable)
  - form: `sort=field,ASC|DESC` or `sort=field`
  - sanitized by allowlist (`@TchPaging.allowedSort`)
  - if all sort fields are not allowed, server falls back to `@TchPaging.defaultSort`
  - default direction: `@TchPaging.defaultDirection` (DESC)

**Normalization note (normative):**

- Invalid or out-of-range numeric pagination values MAY be normalized to defaults/clamped
  (consistent with the project paging resolver).

#### Filters (all optional; combined with logical AND)

- `created_from` (ISO-8601 instant)
- `created_to` (ISO-8601 instant; MUST be ≥ `created_from`)
- `terminal_id` (typed id)
- `draw_id` (typed id)
- `sale_status` (`SOLD` | `PENDING_APPROVAL` | `REJECTED` | `VOID`)
- `result_status` (`NOT_RESULTED` | `WON` | `LOST` | `OVERRIDDEN`)

Constraints:

- `tenant_id` MUST NOT be accepted in query or body.
- Default ordering MUST be `created_at DESC` (enforced by default sort configuration).
- Filters are combined using logical AND.

---

## Response schema (normative)

### Success envelope

- Success responses MUST return `ApiResponse<TchPage<TicketSummary>>`.

### TicketSummary (items[])

Each item includes:

- `ticket_id` (typed id)
- `ticket_code` (string; per-tenant unique; always present)
- `public_code` (string; globally unique; always present in MVP)
- `sale_status`
- `result_status`
- `settlement_status`
- `currency`
- `total_amount` (decimal, scale 2)
- `created_at` (instant)
- `updated_at` (instant)

### TchPage metadata (project standard)

- `page` (int)
- `size` (int)
- `totalElements` (long)
- `totalPages` (int)
- `last` (boolean)
- `hasNext` (boolean)
- `hasPrevious` (boolean)

Normative note:

- An empty `items[]` array is a valid successful response.

---

## HTTP status mapping (normative)

| Status          | Meaning                                                                           |
| --------------- | --------------------------------------------------------------------------------- |
| 200 OK          | List returned successfully (`ApiResponse<TchPage<TicketSummary>>`)                |
| 400 Bad Request | Invalid filter parameters (e.g., `created_to < created_from`)                     |
| 403 Forbidden   | Actor lacks permission _(optional later if enforced at controller/service layer)_ |

---

## Business rules (normative)

### Access and scope

- Listing is strictly tenant-scoped.
- Actor MUST have permission to list tickets within the tenant.

### Sorting

- Default ordering is `created_at DESC`.
- Sort fields MUST be allowlisted (`@TchPaging.allowedSort`).
- Non-allowlisted sort fields are ignored and fallback sort applies.

### Date filters

- Date filters are evaluated in UTC instants.
- `created_from` and `created_to` are inclusive.
- If `created_to < created_from`, request MUST be rejected with `400`.

---

## Errors — ProblemDetail (canonical)

This spec MUST reuse canonical codes from `SALES Index (ACTED)`.

### 400 Bad Request

- `ticket.invalid`
  - Use `violations[]` to report invalid query parameters (e.g., date range)

### 403 Forbidden (optional later)

- `auth.forbidden`

#### Validation error shape (400)

ProblemDetail MAY include:

```json
{
  "type": "about:blank",
  "title": "Validation error",
  "status": 400,
  "code": "ticket.invalid",
  "detail": "Invalid query parameters",
  "violations": [
    { "path": "created_to", "code": "date_range", "message": "created_to must be >= created_from" }
  ]
}
```

---

## Audit (normative)

On each successful list operation (200 OK), the server MUST emit an audit entry:

- `action`: `list_tickets`
- `actor`: derived from auth/context
- `scope`: tenant
- `timestamp`

---

## Events

No AfterCommit events are required for list operations.

---

## Examples

### Example request

```http
GET /api/v1/tenant/tickets?page=0&size=20&sort=created_at,DESC&created_from=2025-12-01T00:00:00Z&created_to=2025-12-31T23:59:59Z&terminal_id=term_01J2...
Authorization: Bearer <token>
```

### Example response (200 OK)

```json
{
  "data": {
    "items": [
      {
        "ticket_id": "tick_01J2Z8WQ...",
        "ticket_code": "TCK-260113-214501-9K3W2H-7",
        "public_code": "9Q2H7M4K1PZX",
        "sale_status": "SOLD",
        "result_status": "NOT_RESULTED",
        "settlement_status": "UNSETTLED",
        "currency": "HTG",
        "total_amount": "35.00",
        "created_at": "2025-12-01T12:34:56Z",
        "updated_at": "2025-12-01T12:35:10Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 148,
    "totalPages": 8,
    "last": false,
    "hasNext": true,
    "hasPrevious": false
  }
}
```
