# OpenSpec — SALES / Public Verify Ticket — Index

## Purpose

Allow public verification of a ticket by `public_code`:

- Single verify (GET)
- Batch verify for multiple codes (POST)

Returns masked public snapshots with per-code statuses and does not expose internal IDs.

## Scope

- Backend (normative, timeless): `01-backend.md`
- Frontend/Terminal/Mobile (informative): `02-frontend.md`, `03-terminal.md`, `04-mobile.md`

Optional public sub-structure (informative only):

- `public/single/00-index.md`
- `public/batch/00-index.md`

## Endpoint(s)

- GET `/api/v1/public/tickets/verify/{public_code}` (public, no auth)
- POST `/api/v1/public/tickets/verify` (public, no auth; batch)

## Public constraints (normative)

- HTTPS only
- Rate-limited
- Noindex
- Visibility window is tenant/outlet configured (e.g., 7–30 days)
  - Outside window MUST return `EXPIRED` (not 404)

## Public state mapping (normative)

- internal `sale_status=VOID` → public `VOID`
- internal `sale_status=PENDING_APPROVAL` → public `PENDING_SYNC`
- expired window → public `EXPIRED`
- otherwise → public `VALID`

## Canonical errors (backend)

- Single:
  - `404` `sales.public_verify_ticket.not_found`
  - `429` `sales.public_verify_ticket.rate_limited`
- Batch:
  - `400` `sales.public_verify_ticket.batch.invalid_request`
  - `429` `sales.public_verify_ticket.rate_limited`
  - Per-item: `sales.public_verify_ticket.invalid`, `sales.public_verify_ticket.not_found`, `sales.public_verify_ticket.expired`

## References

- `openspec/context/10-non-negotiables.md`
- `openspec/context/50-edge-service-rules.md`
- `openspec/context/71-domain-sales.md`
- `openspec/context/26-ticket-codes.md`
