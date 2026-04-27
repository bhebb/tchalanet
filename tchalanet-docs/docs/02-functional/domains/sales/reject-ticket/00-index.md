# OpenSpec — SALES / Reject Ticket — Index

Purpose:

- Reject a `PENDING_APPROVAL` ticket; transition to `REJECTED`; store reason; audit and event.

Scope:

- Backend (normative, timeless): `01-backend.md`
- Frontend (informative): `02-frontend.md`
- Terminal (informative, optional): `03-terminal.md`

Endpoint(s):

- POST `/api/v1/tenant/tickets/{ticket_id}/reject`

Request (minimal fields):

- `reason_code` (required), `reason_note` (optional, max 500)

Preconditions:

- Ticket exists, `sale_status = PENDING_APPROVAL`, not `VOID`, `UNSETTLED`.

Errors (canonical):

- `404` `sales.reject_ticket.not_found`
- `403` `sales.reject_ticket.forbidden`
- `409` `sales.reject_ticket.state_conflict`
- `422` `sales.reject_ticket.invalid`

References:

- `openspec/context/10-non-negotiables.md`
- `openspec/context/71-domain-sales.md`
- `openspec/context/20-backend-rules.md`
