# OpenSpec — SALES / List Tickets — Index

## Purpose

Provide a paged listing of tickets within tenant scope for operational use
(cashiers, operators, administrators).

The list returns **ticket summaries only**, ordered by creation time,
and supports filtering by key operational dimensions.

---

## Scope

- Backend (normative, timeless): `01-backend.md`
- Frontend (informative): `02-frontend.md`
- Terminal (informative): `03-terminal.md`
- Mobile (informative): `04-mobile.md`

---

## Endpoint(s)

- `GET /api/v1/tenant/tickets`

---

## Pagination

- pagination via `@TchPaging` (offset-based: page/size/sort), no cursor.
- MUST follow the project pagination standard.
- Mixing pagination styles in a single request is forbidden.

---

## Filters (minimal normative set)

- `created_from`
- `created_to`
- `terminal_id`
- `draw_id`
- `sale_status`
- `result_status`

All filters are optional and combined conjunctively (AND).

---

## Functional rules (summary)

- Listing is tenant-scoped and read-only.
- Default sort order is `created_at DESC`.
- An empty result set is a valid outcome.
- Ticket lines are not included; details require a dedicated endpoint.

---

## Context packs

- `openspec/context/10-non-negotiables.md`
- `openspec/context/20-backend-rules.md`
- `openspec/context/71-domain-sales.md`
- `openspec/context/26-ticket-codes.md`
- `openspec/context/30-frontend-rules.md`
