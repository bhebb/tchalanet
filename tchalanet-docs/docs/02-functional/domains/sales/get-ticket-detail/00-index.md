# OpenSpec — SALES / Get Ticket Detail — Index

Purpose:

- Return full ticket details by `ticket_id` (tenant scope).
- Return a masked snapshot by `public_code` via the **public verify** API (public scope).

Scope:

- Backend (normative, timeless): `01-backend.md`
- Frontend (informative): `02-frontend.md`
- Terminal (informative): `03-terminal.md`
- Mobile (informative): `04-mobile.md`

Endpoint(s):

- GET `/api/v1/tenant/tickets/{ticket_id}`
- GET `/api/v1/public/tickets/verify/{publicCode}` (masked, rate-limited, noindex)

Context packs:

- `openspec/context/10-non-negotiables.md`
- `openspec/context/20-backend-rules.md`
- `openspec/context/71-domain-sales.md`
- `openspec/context/26-ticket-codes.md`
- `openspec/context/30-frontend-rules.md` (for UI)

Cross-references:

- `openspec/specs/sales/public-verify-ticket/01-backend.md` (public verify constraints)

Notes:

- Public access is served through the canonical **public verify** contract.
  This spec does not introduce a new public “get by public_code” route.
