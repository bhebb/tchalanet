# OpenSpec — SALES / List Tickets — Frontend (v1) — NON-NORMATIVE

References:

- openspec/context/10-non-negotiables.md
- openspec/context/30-frontend-rules.md
- openspec/context/71-domain-sales.md
- openspec/specs/sales/list-tickets/01-backend.md

---

## Purpose

Provide a UI page to list tickets within tenant scope with filters and pagination, default sort by `created_at DESC`. Show ticket summaries and quick actions where permitted.

---

## Views

### Ticket List Page

- Filters bar: created date range (`created_from`, `created_to`), `terminal_id`, `draw_id`, `sale_status`, `result_status`.
- Table/grid:
  - Columns: ticket_code, public_code, sale_status, result_status, settlement_status, currency, total_amount, created_at.
  - Row actions: View detail, Print, Cancel (if authorized).
- Pagination controls: follow project standard (offset or cursor), default sort `created_at DESC`.
- Empty state: guidance and clear CTA to adjust filters.

---

## Data contract

- Bind to `ListTicketsResponse` from backend.
- Each row maps to `TicketSummary` fields (no lines by default).
- Do not duplicate backend logic; backend is source of truth.
- Typed IDs used internally; do not show or require `tenant_id` from body.

---

## Interactions

- Fetch list on load with default sort `created_at DESC`.
- Apply filters via query params; combine conjunctively (AND) in requests.
- Pagination:
  - Offset style: use `page` and `size`; show `total`, `has_next`.
  - Cursor style: use `cursor` and `size`; show `next_cursor` when present.
- Quick actions:
  - View detail: navigate to ticket detail screen.
  - Print: call printable endpoint appropriate to platform.
  - Cancel: call cancel endpoint (if authorized) and refresh list.

---

## Error handling

Map HTTP status and canonical codes to UX messages:

- 403 `sales.list_tickets.forbidden` → "Accès refusé"
- 400 `sales.list_tickets.invalid_filter` → "Filtres invalides"

Display ProblemDetail fields when appropriate:

- title/detail for general context
- violations[] to highlight which filter is invalid (e.g., size range, date order)

---

## Performance

- Debounce filter changes to avoid excessive network calls.
- Virtualize long lists; page size reasonable (e.g., 20–50).
- Preserve scroll position when returning from detail.

---

## Accessibility

- Table headers with accessible labels; sort indicators with text and icons.
- Keyboard navigation support for filters and lists.
- Sufficient contrast for status badges.

---

## Examples (UI data bindings)

Row binding sample:

```json
{
  "ticket_id": "tick_01J2Z8WQ...",
  "ticket_code": "A1B2-C3D4-E5",
  "public_code": "PUB-9X2Z7Q4N1M",
  "sale_status": "SOLD",
  "result_status": "NOT_RESULTED",
  "settlement_status": "UNSETTLED",
  "currency": "HTG",
  "total_amount": "35.00",
  "created_at": "2025-12-01T12:34:56Z"
}
```

Pagination (offset) sample:

```json
{
  "page": 0,
  "size": 20,
  "has_next": true,
  "total": 148
}
```
