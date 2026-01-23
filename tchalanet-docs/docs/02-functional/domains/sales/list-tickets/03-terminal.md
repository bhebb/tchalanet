# OpenSpec — SALES / List Tickets — Terminal (v1) — NON-NORMATIVE

References:

- openspec/context/10-non-negotiables.md
- openspec/context/71-domain-sales.md
- openspec/context/40-mobile-rules.md (if terminal UI shares constraints)
- openspec/specs/sales/list-tickets/01-backend.md

---

## Purpose

Provide terminal UI/flows to list tickets within tenant scope with filters and pagination, default sort by `created_at DESC`. Display ticket summaries and quick actions suitable for terminal form factor.

---

## Views

### Ticket List View (terminal)

- Filters panel:
  - `created_from`, `created_to` (date/time pickers)
  - `terminal_id` (optional)
  - `draw_id` (optional)
  - `sale_status` (optional)
  - `result_status` (optional)
- Results table (fixed-width, readable on small screens):
  - Columns: `ticket_code`, `public_code`, `sale_status`, `result_status`, `settlement_status`, `currency`, `total_amount`, `created_at`.
- Row actions:
  - View detail (navigates to ticket detail)
  - Print (routes to print flow)
  - Cancel (visible only if authorized)
- Pagination controls:
  - Offset style: page number, size selector, next/prev
  - Cursor style: next button when `next_cursor` present

---

## Interactions

- Fetch list: GET `/api/v1/tenant/tickets` with query params (conjunctive AND for filters) and default sort `created_at DESC`.
- Update filters: debounce inputs; re-query with updated params.
- Pagination:
  - Offset: update `page` and `size`; respect `has_next` and `total`.
  - Cursor: pass `cursor` and `size`; follow `next_cursor`.
- Quick actions:
  - View detail: navigate to detail screen.
  - Print: start printing flow.
  - Cancel: call cancel endpoint (if authorized) then refresh list.

---

## Error handling

Map HTTP status and canonical error codes to terminal messages:

- 403 `sales.list_tickets.forbidden` → "Accès refusé"
- 400 `sales.list_tickets.invalid_filter` → "Filtres invalides"

Display ProblemDetail fields when useful:

- `detail` as message body; show `violations[]` inline near corresponding filter (e.g., `size`, `created_to`).

---

## Performance & UX constraints

- Optimize for low-powered terminals; avoid excessive network calls.
- Debounce filter changes and paginate reasonably (size 20–50).
- Use fixed-width fonts for amounts/odds if it improves readability.
- Preserve selection and scroll position on refresh.

---

## Accessibility

- High-contrast badges and clear textual status labels.
- Large touch targets; hardware-key navigation compatible.
- Announce pagination changes to assistive tech.

---

## Example payloads (bindings)

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

Pagination (cursor) sample:

```json
{
  "next_cursor": "eyJwYWdlIjozfQ==",
  "size": 50
}
```
