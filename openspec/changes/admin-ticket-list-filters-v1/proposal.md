# admin-ticket-list-filters-v1

## Why

The admin ticket list must be usable for support work: operators need to narrow tickets by status, date range, code fragment, and stable sort order.

## What

- Add URL-driven admin ticket filters in `admin-portal`: status, ticket/public code search, placed date range, sort, page and size.
- Extend the existing cashier ticket list endpoint used by the admin page with the same query filters.
- Add partial code search against `ticketCode` and `publicCode` in the sales ticket query.

## Non-goals

- No global cache or key invalidation.
- No saved filter presets.
- No new ticket list endpoint.

