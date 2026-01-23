# OpenSpec — SALES / Public Verify — Single — Frontend (v1) — NON-NORMATIVE

References:

- openspec/context/10-non-negotiables.md
- openspec/context/30-frontend-rules.md
- openspec/context/50-edge-service-rules.md
- openspec/specs/sales/public-verify-ticket/01-backend.md

---

## Purpose

Provide a public UI to verify a ticket by `public_code`, showing a masked snapshot and respecting security constraints (HTTPS, noindex, rate-limits).

---

## Views

### Public Verify Page (single)

- Input: `public_code` field
- Result area:
  - Show `public_state` (VALID | VOID | PENDING_SYNC | EXPIRED)
  - Display safe subset: currency, total_amount, draw info, lines minimal, outlet masked
- Error messages:
  - NOT_FOUND → "Code introuvable"
  - Rate-limited → "Trop de requêtes, réessayez plus tard"

---

## Data contract

- GET `/api/v1/public/tickets/verify/{public_code}`
- Bind to `TicketPublicVerifyResponse`

---

## Interactions

- Submit code → fetch → render snapshot
- Respect rate limits; backoff on 429
- Ensure page is marked `noindex`

---

## Error handling

- 404 `sales.public_verify_ticket.not_found`
- 429 `sales.public_verify_ticket.rate_limited`

---

## Privacy & constraints

- HTTPS only; no auth; no PII; outlet masked.

---

## Accessibility

- Clear labels for input; status badges with text.
