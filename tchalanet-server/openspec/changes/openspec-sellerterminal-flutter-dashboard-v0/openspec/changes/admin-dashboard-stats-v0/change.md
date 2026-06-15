# Change: Admin Dashboard & Stats V0

## Status

Proposed

## Why

The operator's admin view is control-oriented. They care about:

- total sales;
- amount to pay;
- seller commissions;
- active/inactive/blocked agents;
- sales by seller/terminal;
- winning/paid/eliminated tickets;
- exposure and limits.

They do not want raw `ticket_line` tables as the primary interface. They want summaries, control rows, and reports.

## What Changes

- Define V0 admin dashboard and reporting surfaces.
- Use SellerTerminal as the main sales/control dimension.
- Add summaries for sales, commissions, payouts, results, exposure, and terminal status.
- Keep dashboards read-only.
- Keep business rules in core domains.

## Scope

Backend:

- `features.dashboard`
- `features.reporting` or existing admin BFF slices
- queries from:
  - `core.sales`
  - `core.terminal`
  - `core.limitpolicy`
  - `core.drawresult`
  - `core.payout`
  - `core.pricing`

Frontend:

- admin dashboard model contracts;
- menu/nav updates;
- reporting pages.

Out of scope:

- complex analytics warehouse;
- BI tooling;
- scheduled report emails;
- dashboard customization engine;
- raw ticket-line admin-first UI.
