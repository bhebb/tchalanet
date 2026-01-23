# OpenSpec — SALES / Public Verify — Batch — Backend (v1)

References:

- openspec/context/05-version-guard.md
- openspec/context/10-non-negotiables.md
- openspec/context/50-edge-service-rules.md
- openspec/context/71-domain-sales.md

---

## Endpoint(s)

- POST /api/v1/public/tickets/verify

---

## Purpose

Allow public verification of multiple tickets by `public_code` in a single request.
Return per-code results and do not fail the whole batch on partial failures.

---

## Headers

### Required

- None (public; no auth)

### Optional

- X-Request-Id

Security constraints (normative):

- HTTPS only.
- Must be rate-limited.
- Must be noindex.

---

## Request schema (normative)

### PublicVerifyBatchRequest

- Body:
  - `public_codes[]` (array of string; min 1; max 20 for MVP)

Constraints:

- `tenant_id` MUST NOT be accepted in body.
- Empty lists or lists exceeding the maximum MUST return `400 sales.public_verify_ticket.batch.invalid_request`.

---

## Response schema (normative)

### PublicVerifyBatchResponse

- `items[]` (array) — one item per requested `public_code`:
  - `public_code` (string)
  - `status` (`OK` | `NOT_FOUND` | `INVALID` | `EXPIRED`)
  - `ticket` (present only when `status = OK`; safe subset like single verify)
  - `error_code` (present only when `status != OK`; canonical `sales.public_verify_ticket.*`)

Visibility window (normative):

- Tenant-configured (e.g., 7–30 days). Outside window → per-item `status = EXPIRED`.

---

## HTTP status mapping (normative)

- `200 OK` → Batch processed; items contain per-code results; partial failures allowed
- `400 Bad Request` → Invalid batch request (empty/too many)
- `429 Too Many Requests` → Rate limit exceeded

---

## Business rules (normative)

### Per-item mapping

| Condition                                         | Item result                                                               |
| ------------------------------------------------- | ------------------------------------------------------------------------- |
| Code exists within visibility window              | `status = OK`; `ticket.public_state = VALID`                              |
| Code exists with `sale_status = VOID`             | `status = OK`; `ticket.public_state = VOID`                               |
| Code exists with `sale_status = PENDING_APPROVAL` | `status = OK`; `ticket.public_state = PENDING_SYNC`                       |
| Code exists but outside visibility window         | `status = EXPIRED`                                                        |
| Code invalid format                               | `status = INVALID`; `error_code = sales.public_verify_ticket.invalid`     |
| Code unknown                                      | `status = NOT_FOUND`; `error_code = sales.public_verify_ticket.not_found` |

### Privacy & masking

- Mask outlet details; avoid sensitive PII.
- Include only a safe subset of ticket fields (amount, minimal lines, draw info).

---

## Errors — ProblemDetail (canonical)

Error code convention:

- `sales.public_verify_ticket.<reason>`

### 400 Bad Request

- `sales.public_verify_ticket.batch.invalid_request`

### 429 Too Many Requests

- `sales.public_verify_ticket.rate_limited`

### Per-item error codes

- `sales.public_verify_ticket.invalid`
- `sales.public_verify_ticket.not_found`

---

## Events & audit

### Audit (informative)

- `action`: `public_verify_ticket_batch`
- `resource`: `count` (number of codes)
- `timestamp`

### AfterCommit events

- None required (public read).

---

## Examples

### Example request

```http
POST /api/v1/public/tickets/verify
Content-Type: application/json

{
  "public_codes": [
    "PUB-9X2Z7Q4N1M",
    "PUB-INVALID",
    "PUB-9X2Z7Q4N1X"
  ]
}
```

### Example response (200)

```json
{
  "items": [
    {
      "public_code": "PUB-9X2Z7Q4N1M",
      "status": "OK",
      "ticket": {
        "public_state": "VALID",
        "public_code": "PUB-9X2Z7Q4N1M",
        "currency": "HTG",
        "total_amount": "35.00"
      }
    },
    {
      "public_code": "PUB-INVALID",
      "status": "INVALID",
      "error_code": "sales.public_verify_ticket.invalid"
    },
    {
      "public_code": "PUB-9X2Z7Q4N1X",
      "status": "NOT_FOUND",
      "error_code": "sales.public_verify_ticket.not_found"
    }
  ]
}
```

### Example error (400 invalid request)

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "public_codes must contain between 1 and 20 items",
  "code": "sales.public_verify_ticket.batch.invalid_request"
}
```
