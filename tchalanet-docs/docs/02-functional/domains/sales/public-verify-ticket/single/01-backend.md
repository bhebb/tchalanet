# OpenSpec — SALES / Public Verify — Single — Backend (v1)

References:

- openspec/context/05-version-guard.md
- openspec/context/10-non-negotiables.md
- openspec/context/50-edge-service-rules.md
- openspec/context/71-domain-sales.md

---

## Endpoint(s)

- GET /api/v1/public/tickets/verify/{public_code}

---

## Purpose

Allow public verification of a single ticket by its `public_code`. Return a masked public-facing snapshot and enforce public constraints (HTTPS, rate-limited, noindex).

---

## Headers

### Required

- None (public; no auth)

### Optional

- X-Request-Id

Security constraints (normative):

- HTTPS only (see non-negotiables).
- Must be rate-limited.
- Must be noindex.

---

## Request schema (normative)

### PublicVerifyTicketRequest

- Path: `public_code` (string; globally unique; non-guessable)

Constraints:

- `tenant_id` MUST NOT be accepted in body.

---

## Response schema (normative)

### TicketPublicVerifyResponse

- `public_state` (`VALID` | `VOID` | `PENDING_SYNC` | `EXPIRED`)
- `public_code` (string)
- `currency` (string)
- `total_amount` (decimal)
- `draw` (object):
  - `occurred_at` (instant, optional)
  - `next_at` (instant, optional)
- `lines[]` (safe subset)
- `outlet` (masked, partial)
- `share_link` (optional)

Visibility window (normative):

- Tenant-configured (e.g., 7–30 days). Outside window → `EXPIRED` (not 404).

---

## HTTP status mapping (normative)

- 200 OK → Public snapshot returned (VALID/VOID/PENDING_SYNC/EXPIRED)
- 404 Not Found → Code invalid/unknown
- 429 Too Many Requests → Rate limit exceeded

---

## Business rules (normative)

### Internal → Public mapping

| Internal                         | Public                        |
| -------------------------------- | ----------------------------- |
| `sale_status = VOID`             | `public_state = VOID`         |
| `sale_status = PENDING_APPROVAL` | `public_state = PENDING_SYNC` |
| Within visibility window         | `public_state = VALID`        |
| Outside visibility window        | `public_state = EXPIRED`      |

### Privacy & masking

- Mask outlet details and avoid sensitive PII.
- Include only a safe subset of lines/numbers.

### Decision table — Verify outcome

| Condition                     | Result                                        |
| ----------------------------- | --------------------------------------------- |
| `public_code` exists          | 200 snapshot                                  |
| `public_code` invalid/unknown | 404 `sales.public_verify_ticket.not_found`    |
| Rate limit exceeded           | 429 `sales.public_verify_ticket.rate_limited` |

---

## Errors — ProblemDetail (canonical)

Error code convention:

- `sales.public_verify_ticket.<reason>`

### 404 Not Found

- `sales.public_verify_ticket.not_found`

### 429 Too Many Requests

- `sales.public_verify_ticket.rate_limited`

---

## Events & audit

### Audit (informative)

- action: `public_verify_ticket`
- resource: `public_code`
- timestamp

### AfterCommit events

- None required (public read).

---

## Examples

### Example request

```http
GET /api/v1/public/tickets/verify/PUB-9X2Z7Q4N1M
X-Request-Id: 6f8d0a1e-3c4b-4f9e-bd2a-1b2c3d4e5f6a
```

### Example response (200 VALID)

```json
{
  "public_state": "VALID",
  "public_code": "PUB-9X2Z7Q4N1M",
  "currency": "HTG",
  "total_amount": "35.00",
  "draw": {
    "occurred_at": "2025-12-02T18:00:00Z",
    "next_at": "2025-12-09T18:00:00Z"
  },
  "lines": [{ "selection": "Team A vs Team B — WIN A", "odds": "1.80" }],
  "outlet": { "name_masked": "Outlet **5" }
}
```
