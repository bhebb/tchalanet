# Claude/Codex Prompts

## General instruction

Read:

- `AGENTS.md`
- `VERSIONS.md`
- `openspec/AGENTS.md`
- `docs/ARCHITECTURE.md`
- `docs/PLAYBOOK.md`
- relevant `docs/conventions/*`
- the specific OpenSpec change folder

Do not implement feature endpoints as replacements for core CRUD/read endpoints.

## Prompt 00

Implement `00-db-reset-core-operational-sales` exactly. Since the DB can be recreated, replace obsolete Flyway scripts instead of preserving backwards compatibility. Keep RLS, typed IDs, and ddl validation.

## Prompt 01

Implement `01-core-outlet-operational-context`. Core outlet owns CRUD/read/operational endpoints. Do not move outlet CRUD to tenantadmin feature.

## Prompt 02

Implement `02-core-terminal-runtime`. Core terminal owns CRUD/runtime/status/sync endpoints. Fix request DTOs, raw UUIDs, actor from context, and command bus usage.

## Prompt 03

Implement `03-core-session-sales-session`. SalesSession is seller-scoped. Current session is by current user, not terminal. Payout after selling session close is allowed.

## Prompt 03b

Implement `03b-core-session-draw-lifecycle-auto-open-close`. Add session automation tied to draw lifecycle safely. Do not make sessions terminal-scoped. Add admin/ops preview/status endpoints. Auto-close after last sellable draw/day, not after payout.

## Prompt 04

Implement `04-core-limitpolicy-autonomy`. Targets are TENANT/OUTLET/USER only in MVP. LimitPolicy decides ALLOW/WARN/REQUIRE_APPROVAL/BLOCK. Autonomy decides approval role/level.

## Prompt 05

Implement `05-core-sales-alignment`. SellTicket must validate session/outlet/terminal/draw/pricing/policy and attach session to ticket.

## Prompt 06

Implement `06-core-payout-alignment`. Payout must not require selling session to remain open. Remove tenantId from bodies.

## Prompt 07

Implement `07-features-tenantadmin-onboarding-overview`. Keep it orchestration only. Do not duplicate core CRUD.

## Prompt 08

Implement `08-features-cashier-seller-flow`. Keep it BFF orchestration only. Sell delegates to core.sales.

## Prompt 09

Implement cleanup. Delete obsolete code and run architecture checks.
