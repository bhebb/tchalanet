# Ticket codes (ticket_code vs public_code)

- openspec/context/05-version-guard.md
- openspec/context/10-non-negotiables.md

## Goal

Define the two identifiers used for tickets:

- `ticket_code`: internal human-friendly identifier for ops/support/search
- `public_code`: public shareable code for verification (QR/SMS/URL)

These codes are stable and part of the public/tenant API contracts.

---

## Definitions

### ticket_code (internal / tenant)

- Purpose:
  - Support, troubleshooting, and search in OPS / tenant backoffice
  - Human typing friendly (includes a check digit)
- Scope:
  - **Unique per tenant**
- Availability:
  - **Always present** on tenant endpoints (sell/list/detail/print)
- Stability:
  - Immutable once created
- Example (informative):
  - `TCK-260113-214501-9K3W2H-7`

### public_code (public verification)

- Purpose:
  - Public ticket verification link (QR/SMS/URL)
  - Must be short and non-guessable
- Scope:
  - **Globally unique** (because public verify route has no tenant path segment)
- Availability:
  - **Always present in MVP** (recommended)
- Stability:
  - Immutable once created
- Encoding (informative):
  - Crockford Base32 (no ambiguous chars)
- Example (informative):
  - `9Q2H7M4K1PZX`

---

## Public verification contract

- Public verify endpoint uses `public_code`:
  - `GET /api/v1/public/tickets/verify/{publicCode}`
- Security constraints (normative):
  - Data returned MUST be masked (no sensitive fields)
  - Response MUST be `noindex`
  - Server MUST apply rate limiting
  - HTTPS only

---

## Collision handling (informative)

- `ticket_code` and `public_code` MUST have DB unique constraints.
- On rare collisions, server retries generation until persisted.

---

## Non-goals

- This document does not define ticket rendering formats (ESC/POS/PDF).
- This document does not define short-link services.
