# Change: DB Reset — Core Operational Sales Runtime

## Why

Current sales runtime tables and controllers are inconsistent with the finalized MVP boundaries:

- `core.outlet` is operational sales context, not passive catalog.
- `core.terminal` is runtime device/sync/offline state.
- `core.session` owns seller-scoped `SalesSession`.
- `core.sales` owns ticket sale/print/cancel.
- `core.payout` owns payout workflow.
- `core.limitpolicy` and `core.autonomy` own limits/approval.
- Features should not replace core CRUD/read endpoints.

The DB may be recreated, so this change may replace the current Flyway scripts instead of writing backward-compatible migrations.

## What

Recreate canonical tables and constraints for:

- outlet
- outlet_user
- terminal
- terminal_user_assignment
- sales_session
- sales_session_totals
- ticket
- ticket_line
- payout
- limit_definition
- limit_assignment
- autonomy_policy_rule
- approval_request

## Non-goals

- Do not implement draw refactor here.
- Do not create PageModel providers here.
- Do not move core CRUD into features.
