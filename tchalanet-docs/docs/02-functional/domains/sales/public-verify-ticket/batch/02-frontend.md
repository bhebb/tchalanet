# OpenSpec — SALES / Public Verify — Batch — Frontend (v1) — NON-NORMATIVE

References:

- openspec/context/10-non-negotiables.md
- openspec/context/30-frontend-rules.md
- openspec/context/50-edge-service-rules.md
- openspec/specs/sales/public-verify-ticket/01-backend.md

---

## Purpose

Provide a public UI to verify multiple `public_code`s in batch, showing per-code results and respecting constraints (HTTPS, noindex, rate-limits).

---

## Views

### Public Verify Batch Page

- Input: list/textarea of `public_code`s (min 1, max 20)
- Result table:
  - Columns: public_code, status (OK | NOT_FOUND | INVALID | EXPIRED), snippet of ticket data when OK
- Error messages:
  - Invalid request (empty or too many): show validation
  - Rate-limited: message and suggest retry later

---

## Data contract

- POST `/api/v1/public/tickets/verify`
- Request: `{ public_codes: string[] }`
- Response: `{ items: [...] }` per-code

---

## Interactions

- Submit list → POST → render items
- Partial failures do not block rendering of other results
- Respect rate limits; backoff on 429
- Ensure page marked `noindex`

---

## Error handling

- 400 `sales.public_verify_ticket.batch.invalid_request`
- 429 `sales.public_verify_ticket.rate_limited`
- Per-item codes: `sales.public_verify_ticket.invalid`, `sales.public_verify_ticket.not_found`

---

## Privacy & constraints

- HTTPS only; no auth; no PII; outlet masked.
- Do not display internal identifiers; show only masked/safe subset for OK items.

---

## Accessibility

- Input helps (counter up to 20); table has accessible headers.
- Clear status labels with text and color; screen-reader friendly.

---

## Examples

### Example request (batch)

```json
{
  "public_codes": ["PUB-9X2Z7Q4N1M", "PUB-INVALID", "PUB-9X2Z7Q4N1X"]
}
```

### Example response (200 batch)

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
