# OpenSpec — SALES / Void Ticket — Index

Purpose:

- Void (cancel) a ticket; transition `sale_status` to `VOID` for authorized actors; audit and event.

Scope:

- Backend (normative, timeless): `01-backend.md`
- Frontend (informative): `02-frontend.md`
- Terminal (informative, optional): `03-terminal.md`

Endpoint(s):

- POST `/api/v1/tenant/tickets/{ticket_id}/void`

Request (minimal fields):

- `reason_code` (required), `reason_note` (optional, max 500)

Preconditions:

- Ticket exists; `settlement_status != SETTLED` (v1). If `SETTLED`, reject with `409 state_conflict`.

Errors (canonical):

- `404` `sales.void_ticket.not_found`
- `403` `sales.void_ticket.forbidden`
- `409` `sales.void_ticket.state_conflict`
- `422` `sales.void_ticket.invalid`

References:

- `openspec/context/10-non-negotiables.md`
- `openspec/context/71-domain-sales.md`
- `openspec/context/20-backend-rules.md`
