# OpenSpec — SALES / Public Verify — Single — Index

Purpose:

- Single public verification of a ticket by `public_code`.

Scope:

- Backend (normative reference): `../../01-backend.md`
- Frontend/Terminal/Mobile (informative): may add `02-frontend.md`, `03-terminal.md`, `04-mobile.md` here.

Endpoint:

- GET `/api/v1/public/tickets/verify/{public_code}` (no auth; HTTPS; rate-limited; noindex)

Response (snapshot):

- `public_state` (`VALID` | `VOID` | `PENDING_SYNC` | `EXPIRED`), masked outlet, safe lines, currency/amount.

Errors (canonical):

- `404` `sales.public_verify_ticket.not_found`
- `429` `sales.public_verify_ticket.rate_limited`
