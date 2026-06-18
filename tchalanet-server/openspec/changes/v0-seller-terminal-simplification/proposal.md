# Change: v0-seller-terminal-simplification

## Decision

For V0, backend operations SHALL center on `seller_terminal` as the tenant-scoped operational POS actor.

```text
tenant
  -> seller_terminal
       -> POS authentication
       -> ticket sales
       -> block / suspend / disable
       -> commission
       -> limits
       -> odds / baremes override
       -> reporting
```

The V0 backend SHALL stop treating these as independently required runtime concepts for selling:

- separate seller;
- technical terminal;
- outlet;
- sales session;
- payout;
- ledger;
- offline sync;
- autonomy.

Those concepts may remain in parking lot documentation for V1+, but V0 sales, controls and reporting SHALL be expressed through `seller_terminal`.

`seller_terminal` is distinct from `TENANT_ADMIN` and `SUPER_ADMIN`. Seller-terminal POS authentication SHALL use a separate bootstrap path from admin app-user authentication.

## Why

The current model is richer than the immediate product need. It introduces too many tables, handlers, context facts and validation gates before the product has proven those flows.

For V0, the client-facing operational truth is simpler:

- a tenant has seller terminals;
- an active seller terminal can authenticate as a POS actor and sell;
- an admin can block, configure or limit a seller terminal;
- reports aggregate by seller terminal, draw, day and period.

This keeps critical business logic in `core`, leaves BFF orchestration in `features`, avoids invalid `features -> core.internal` dependencies, and avoids expanding `common` beyond neutral runtime primitives.

## What

Introduce a backend simplification plan that:

- creates a real `core.sellerterminal` slice as the V0 operational seller/terminal aggregate;
- migrates existing seller-terminal code from `core.terminal` to `core.sellerterminal`;
- deletes `core.terminal` after import/bean/build verification;
- moves seller-terminal identity bootstrap out of `features.cashier`;
- adds a public `platform.identity.api.SellerTerminalIdentityLookup` SPI implemented by `core.sellerterminal`;
- keeps POS and admin identity paths separate with no fallback between them;
- changes sales to use `SellerTerminalId` as the sell-time operational identity;
- simplifies operational context to carry seller-terminal identity instead of terminal/outlet/session;
- updates admin endpoints to `/admin/seller-terminals`;
- updates POS endpoints to `/tenant/seller-terminal/me` and `/tenant/seller-terminal/operational-context`;
- parks payout, ledger, offline sync, sessions, outlets and autonomy for V1+;
- documents DB, API, event, permission, batch and migration impacts before implementation.

## Impact

Backend docs:

- add `docs/architecture/v0-seller-terminal-simplification.md`;
- update architecture/playbook/convention docs that still present session/outlet/payout/offline/autonomy as V0 active surfaces;
- add an ADR for `SellerTerminal` as V0 operational POS actor.

Backend code, later phases:

- create `core.sellerterminal`;
- migrate and delete `core.terminal`;
- add or adjust tenant-scoped `seller_terminal` persistence with RLS;
- preserve POS bootstrap through seller-terminal identity with no app-user fallback;
- adapt sales command/context/ticket persistence/reporting;
- disable or remove V0-dead controllers, handlers, listeners and jobs;
- update permissions and access control naming.

Database:

- keep `seller_terminal`, `ticket`, `ticket_line`, draw/result, promotion, limit policy, tenant config, audit and idempotency records;
- remove `outlet_id` and optional `address_id` from seller-terminal V0 model;
- add `seller_terminal_id` to tickets and reporting indexes;
- remove or ignore V0-dead tables such as seller, terminal, sales session, payout, ledger, offline sync and autonomy tables where safe for current migration strategy;
- prefer progressive ticket migration: add/backfill `seller_terminal_id`, require it for new sales, then drop old ticket columns only after verification.

## Non-goals

- No frontend route/menu implementation in this change.
- No mobile/POS app changes in this change.
- No payout/ledger/offline/autonomy implementation in V0.
- No replacement of `limitpolicy` with tenant config in this change.
- No destructive DB migration before explicit migration strategy confirmation.
- No fallback from POS identity to admin app-user identity, or from admin identity to seller-terminal identity.

## Success criteria

- The OpenSpec change clearly defines the V0 operational model.
- POS and admin actor boundaries are explicit.
- The invalid `features.cashier` identity adapter ownership is called out with a migration target.
- Follow-up implementation can be split by docs, login/identity, `core.sellerterminal`, context, sales, reports, dead-slice cleanup and verification.
- No backend component is asked to depend on web/mobile scope to complete this simplification.
