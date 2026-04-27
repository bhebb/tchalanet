# OpenSpec — SALES / Public Verify — Batch — Index

Purpose:

- Batch public verification for multiple `public_code`s with partial results.

Scope:

- Backend (normative reference): `../../01-backend.md`
- Frontend/Terminal/Mobile (informative): may add `02-frontend.md`, `03-terminal.md`, `04-mobile.md` here.

Endpoint:

- POST `/api/v1/public/tickets/verify` (no auth; HTTPS; rate-limited; noindex)

Request:

- `{ public_codes: string[] }` — min 1, max 20 (MVP)

Response:

- `200 OK` with `items[]`: each item has `public_code`, `status` (`OK`|`NOT_FOUND`|`INVALID`|`EXPIRED`), `ticket` when OK, and `error_code` otherwise.

Errors (canonical):

- `400` `sales.public_verify_ticket.batch.invalid_request`
- `429` `sales.public_verify_ticket.rate_limited`
