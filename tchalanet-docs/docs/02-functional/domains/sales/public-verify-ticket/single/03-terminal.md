# OpenSpec — SALES / Public Verify — Single — Terminal (v1) — NON-NORMATIVE

References:

- openspec/context/10-non-negotiables.md
- openspec/context/50-edge-service-rules.md
- openspec/specs/sales/public-verify-ticket/01-backend.md

---

## Purpose

Provide terminal/kiosk flows to verify a ticket by `public_code`, showing a masked snapshot, and respecting security constraints (HTTPS, noindex, rate-limits).

---

## Views

### Terminal Verify View (single)

- Input: `public_code`
- Result:
  - `public_state` badge
  - Safe subset fields (amounts, draw, minimal lines, outlet masked)
- Messages:
  - NOT_FOUND: "Code introuvable"
  - Rate-limited: "Trop de requêtes, réessayez plus tard"

---

## Interactions

- GET `/api/v1/public/tickets/verify/{public_code}`
- Backoff on 429; avoid rapid re-requests.
- Ensure device/browser page is `noindex`.

---

## Error handling

- 404 `sales.public_verify_ticket.not_found`
- 429 `sales.public_verify_ticket.rate_limited`

---

## Accessibility

- Large input targets; readable status labels.
