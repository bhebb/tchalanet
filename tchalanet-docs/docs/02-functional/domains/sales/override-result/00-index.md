# OpenSpec — SALES / Override Result — Index

## Purpose

Manually override the result of an existing ticket in exceptional cases
(e.g. official correction, dispute resolution, regulatory decision).

The override records an explicit outcome (`WON` or `LOST`), stores metadata
(reason, actor, timestamp), and emits audit and domain events.

This operation is exceptional and strictly controlled.

---

## Scope

- Backend (normative, timeless): `01-backend.md`
- Frontend (informative): `02-frontend.md`
- Terminal (informative, optional): `03-terminal.md`

---

## Endpoint(s)

- `POST /api/v1/tenant/tickets/{ticket_id}/override-result`

---

## Functional rules (summary)

- Only existing tickets in tenant scope may be overridden.
- A ticket **must not be VOID**.
- A ticket **must be UNSETTLED** (v1 policy).
- Override does **not** settle the ticket and does **not** pay winnings.
- Override is fully audited and traceable.

---

## Errors (canonical)

- `404` `sales.override_result.not_found`
- `403` `sales.override_result.forbidden`
- `409` `sales.override_result.state_conflict`
- `422` `sales.override_result.invalid`

---

## References

- `openspec/context/10-non-negotiables.md`
- `openspec/context/20-backend-rules.md`
- `openspec/context/71-domain-sales.md`
