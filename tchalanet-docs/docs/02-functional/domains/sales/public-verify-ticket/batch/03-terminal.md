# OpenSpec — SALES / Public Verify — Batch — Terminal (v1) — NON-NORMATIVE

References:

- openspec/context/10-non-negotiables.md
- openspec/context/50-edge-service-rules.md
- openspec/specs/sales/public-verify-ticket/01-backend.md

---

## Purpose

Provide terminal/kiosk flows to verify multiple `public_code`s in batch, showing per-code results and respecting constraints (HTTPS, noindex, rate-limits).

---

## Views

### Terminal Verify Batch View

- Input: list/textarea of `public_code`s (min 1, max 20)
- Results list/table (compact):
  - public_code
  - status badge (OK | NOT_FOUND | INVALID | EXPIRED)
  - snippet of ticket data when OK (amount, minimal draw info)
- Messages:
  - Invalid request (empty or too many): validation message
  - Rate-limited: "Trop de requêtes, réessayez plus tard"

---

## Interactions

- POST `/api/v1/public/tickets/verify` with `{ public_codes: string[] }`
- Render each item independently; partial failures do not block others
- Backoff on 429; avoid rapid re-requests
- Ensure terminal page is `noindex`

---

## Error handling

- 400 `sales.public_verify_ticket.batch.invalid_request`
- 429 `sales.public_verify_ticket.rate_limited`
- Per-item: `sales.public_verify_ticket.invalid`, `sales.public_verify_ticket.not_found`

---

## Privacy & constraints

- HTTPS only; no auth; no PII; outlet masked; no internal ids.

---

## Accessibility

- Large input targets; readable status labels; high-contrast badges.

---

## Examples

Request sample:

```json
{
  "public_codes": ["PUB-9X2Z7Q4N1M", "PUB-INVALID"]
}
```

Response sample (200):

```json
{
  "items": [
    {
      "public_code": "PUB-9X2Z7Q4N1M",
      "status": "OK",
      "ticket": {
        "public_state": "VALID",
        "currency": "HTG",
        "total_amount": "35.00"
      }
    },
    {
      "public_code": "PUB-INVALID",
      "status": "INVALID",
      "error_code": "sales.public_verify_ticket.invalid"
    }
  ]
}
```
