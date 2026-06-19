# Tasks: v0-seller-terminal-simplification

## Status

Proposed — validate before destructive implementation.

## Goal

Simplify V0 backend around one operational POS actor: `seller_terminal`.

`seller_terminal` replaces these V0 concepts:

- separate seller;
- technical terminal;
- outlet;
- sales session;
- offline sync;
- autonomy;
- advanced payout/ledger.

```text
Tenant
  -> SellerTerminal
       -> POS authentication
       -> ticket sales
       -> block / suspend / disable
       -> commission
       -> limits
       -> odds / baremes override
       -> reporting
```

## Core decisions

- [ ] `seller_terminal` is the V0 operational POS actor.
- [ ] `seller_terminal` is distinct from `TENANT_ADMIN` and `SUPER_ADMIN` actors.
- [ ] Seller-terminal authentication uses a flow separate from admin authentication.
- [ ] `outlet` is removed from V0.
- [ ] `session` is removed from V0.
- [ ] Existing `core.terminal` migrates to a real `core.sellerterminal` slice.
- [ ] Existing `core.terminal` is deleted after migration and verification.
- [ ] `payout`, `ledger`, `offline sync` and `autonomy` are parking lot V1+.

## Phase 0 — Safety and decision record

- [x] Create backend OpenSpec change `v0-seller-terminal-simplification`.
- [ ] Create backup branch before destructive implementation:
  - `git checkout main`
  - `git pull`
  - `git checkout -b backup/pre-v0-seller-terminal-simplification`
  - `git push origin backup/pre-v0-seller-terminal-simplification`
- [ ] Create implementation branch `refactor/v0-seller-terminal-simplification`.
- [ ] Add ADR: `ADR — V0 SellerTerminal as operational POS actor`.
- [ ] Explicitly mark parking lot V1+: outlet, session, payout, ledger, offline sync, autonomy, old terminal model, separate seller model.
- [x] Decide whether temporary aliases stay active during migration:
  - `GET /tenant/terminal/me`
  - `GET /tenant/me/operational-context`
  - Decision: retained as temporary aliases while new `/tenant/seller-terminal/*` routes are introduced.
- [ ] Freeze new V0 development on payout/offline/session/autonomy.

Acceptance:

- [ ] Backup branch exists on remote.
- [ ] Refactor branch exists.
- [x] OpenSpec exists.
- [ ] Parking lot is documented.
- [ ] No new work targets V0-removed slices.

## Phase 1 — Documentation

- [ ] Add `docs/architecture/v0-seller-terminal-simplification.md`.
- [ ] Update `docs/ARCHITECTURE.md` to replace active V0 surfaces:
  - tenant admin outlets;
  - tenant admin terminals;
  - tenant admin autonomy;
  - tenant admin sessions;
  - tenant admin payouts.
- [ ] Document active V0 surfaces:
  - tenant admin seller terminals;
  - tenant admin limits;
  - tenant admin odds/baremes;
  - tenant admin commissions;
  - tenant admin reports by seller terminal.
- [ ] Update `docs/PLAYBOOK.md` examples from `TerminalId`, `OutletId`, `SalesSessionId` to `SellerTerminalId`.
- [ ] Update `docs/modules/core.md` with `core.sellerterminal` as owner of the V0 operational POS actor.
- [ ] Update `docs/conventions/request_context_usage.md`.
- [ ] Update `docs/conventions/user-contexte-operational.md` if present.
- [ ] Update `docs/conventions/inter_domain_calls.md`:
  - sales -> sellerterminal read boundary;
  - sales must not import sellerterminal internal persistence;
  - sales uses sellerterminal API/query/port or context snapshot.
- [ ] Update `docs/conventions/security_permissions.md` with V0 seller-terminal permissions.
- [ ] Update `docs/conventions/web_api.md` with admin endpoints:
  - `GET /admin/seller-terminals`
  - `POST /admin/seller-terminals`
  - `GET /admin/seller-terminals/{id}`
  - `PUT /admin/seller-terminals/{id}`
  - `PATCH /admin/seller-terminals/{id}/block`
  - `PATCH /admin/seller-terminals/{id}/unblock`
  - `PATCH /admin/seller-terminals/{id}/disable`
  - `PATCH /admin/seller-terminals/{id}/reset-access`
- [ ] Update `docs/conventions/web_api.md` with POS endpoints:
  - `GET /tenant/seller-terminal/me`
  - `GET /tenant/seller-terminal/operational-context`
- [ ] Document temporary POS endpoint aliases if retained:
  - `GET /tenant/terminal/me`
  - `GET /tenant/me/operational-context`
- [ ] Update `docs/conventions/batch.md`:
  - active V0: `draw.generate`, `draw.open_today`, `draw.processing.close`, `drawresult.fetch`, `drawresult.apply`;
  - parking lot V1+: session cleanup, payout workflows, ledger projectors, offline sync jobs, autonomy jobs.
- [ ] Update `docs/conventions/idempotency.md`:
  - `POST /tenant/tickets` keeps `Idempotency-Key`;
  - offline retry semantics are parked V1+.

Acceptance:

- [ ] Docs no longer present outlet/session/payout/offline/autonomy as V0 active flows.
- [ ] Docs present `seller_terminal` as V0 pivot.
- [ ] Backend examples use `SellerTerminalId`.
- [ ] V0 endpoints are documented.
- [ ] Parking lot is clear.

## Phase 2A — Audit seller-terminal login flow

Current flow to document:

```text
Firebase token
  -> ExternalAuthenticatedUser
  -> UserBootstrapFilterImpl
  -> ExpectedActorTypeResolver
  -> X-Tch-Client-Type = POS ?
       yes -> SellerTerminal lookup
       no  -> AppUser lookup
  -> BootstrappedActor
  -> TchContextFilter
  -> TchRequestContext
```

- [ ] Document the real seller-terminal login flow.
- [ ] Verify `ExpectedActorTypeResolver`.
- [ ] Verify `X-Tch-Client-Type = POS` forces `TchActorType.SELLER_TERMINAL`.
- [ ] Verify no fallback exists between POS and AppUser:
  - POS without seller-terminal mapping => 403 `terminal.external_identity_not_linked`;
  - admin without app user mapping => 403 `external_identity.not_linked`;
  - POS never attempts app user lookup;
  - admin never attempts seller-terminal lookup.
- [ ] Verify expected errors:
  - `terminal.external_identity_not_linked`
  - `terminal.not_active`
  - `external_identity.not_linked`
  - `user.not_active`
  - `external_identity.missing_verified_identity`
- [ ] Verify seller-terminal status mapping:
  - `ACTIVE` => allowed;
  - `BLOCKED`, `SUSPENDED`, `DISABLED`, `DELETED` => forbidden.
- [ ] Verify `BootstrappedActor.sellerTerminal(...)` carries:
  - sellerTerminalId;
  - tenantId;
  - provider;
  - issuer;
  - subject;
  - actorType = `SELLER_TERMINAL`.
- [ ] Verify `TchContextFilter` produces:
  - `TchRequestContext.actorType = SELLER_TERMINAL`;
  - `TchRequestContext.sellerTerminalId` non-null;
  - `TchRequestContext.tenantId` = seller terminal tenant;
  - authority `ACTOR_SELLER_TERMINAL`.
- [x] Verify POS endpoints use `@PreAuthorize("hasAuthority('ACTOR_SELLER_TERMINAL')")`.

Tests:

- [ ] POS token linked to active seller-terminal => context OK.
- [ ] POS token not linked => 403 `terminal.external_identity_not_linked`.
- [ ] POS token linked to blocked terminal => 403 `terminal.not_active`.
- [ ] Admin token linked to app user => context OK.
- [ ] Admin token not linked => 403 `external_identity.not_linked`.
- [ ] POS token never falls back to app user.
- [ ] Admin token never falls back to seller-terminal.

## Phase 2B — Move identity adapter out of features

Problem:

```text
features.cashier.sellerterminal.identity.SellerTerminalExternalIdentityAdapter
  imports core.terminal.internal.infra.persistence.sellerterminal.SellerTerminalJpaRepository
```

This is invalid because:

- features must stay leaf/BFF;
- features must not implement identity bootstrap adapters;
- features must not import core internal persistence;
- platform.identity must not depend on features.

Target V0:

```text
platform.identity.api.SellerTerminalIdentityLookup
        ↑ implemented by
core.sellerterminal.internal.infra.identity.CoreSellerTerminalIdentityLookupAdapter
        ↑ uses
core.sellerterminal.internal.infra.persistence.SellerTerminalJpaRepository
```

- [x] Create `platform.identity.api.SellerTerminalIdentityLookup`.
- [x] Create `platform.identity.api.model.SellerTerminalIdentityBootstrapView`.
- [x] Create `platform.identity.api.model.SellerTerminalBootstrapStatus`.
- [x] Modify `UserBootstrapFilterImpl` to inject `SellerTerminalIdentityLookup`.
- [x] Remove dependency on `SellerTerminalExternalIdentityPort` if possible.
- [x] Move adapter from `features.cashier` to `core.sellerterminal.internal.infra.identity`.
- [x] Rename adapter to `CoreSellerTerminalIdentityLookupAdapter`.
- [x] Remove old misplaced adapter from `features.cashier`.
- [x] Add package review/ArchUnit rule where possible:
  - `features..` must not implement platform.identity internal ports;
  - `features..` must not import `core..internal..`;
  - `platform..` must not import `core..`.

Acceptance:

- [x] No identity bootstrap bean lives in `features.cashier`.
- [x] `features.cashier` does not import `core.*.internal.*`.
- [x] `UserBootstrapFilterImpl` depends on public `platform.identity.api` SPI.
- [x] Seller-terminal lookup implementation lives in owning core slice.

## Phase 2C — Create `core.sellerterminal` slice

Target structure:

```text
core/sellerterminal/
  api/
    command/
    query/
    model/
    event/
  internal/
    domain/
      model/
      service/
      event/
    application/
      command/handler/
      query/handler/
      port/out/
    infra/
      persistence/
      identity/
      web/admin/
      web/tenant/
      mapper/
      config/
```

- [x] Create package `com.tchalanet.server.core.sellerterminal`.
- [x] Move/create API models:
  - `SellerTerminalStatus`
  - `SellerTerminalView`
  - `SellerTerminalSummaryRow`
  - `SellerTerminalSearchCriteria`
  - `CurrentOperationalContextView`
- [ ] Move/create commands:
  - [x] `CreateSellerTerminalCommand`
  - [x] `UpdateSellerTerminalCommand`
  - [x] `BlockSellerTerminalCommand`
  - [x] `UnblockSellerTerminalCommand`
  - [x] `DisableSellerTerminalCommand`
  - [x] `ResetSellerTerminalAccessCommand`
  - [x] existing `SetSellerTerminalCommissionRateCommand`
  - [ ] decide whether to add `UpdateSellerTerminalLimitsCommand` wrapper or keep using `limitpolicy`
  - [ ] decide whether to add `UpdateSellerTerminalOddsCommand` wrapper or keep using `pricing`
- [x] Move/create queries:
  - `GetSellerTerminalQuery`
  - `GetSellerTerminalMeQuery`
  - `ListSellerTerminalsQuery`
  - `GetCurrentOperationalContextQuery`
- [ ] Move/create events:
  - `SellerTerminalCreatedEvent`
  - `SellerTerminalUpdatedEvent`
  - `SellerTerminalBlockedEvent`
  - `SellerTerminalUnblockedEvent`
  - `SellerTerminalDisabledEvent`
  - `SellerTerminalAccessResetEvent`
- [ ] Move/create domain:
  - [x] `SellerTerminal`
  - [ ] `SellerTerminalAccess`
  - [ ] `SellerTerminalCommission`
  - [x] existing seller terminal status policy.
- [x] Move/create persistence:
  - `SellerTerminalJpaEntity`
  - `SellerTerminalJpaRepository`
  - `SellerTerminalJpaMapper`
  - `SellerTerminalJpaAdapter`
- [x] Move/create identity adapter `CoreSellerTerminalIdentityLookupAdapter`.
- [x] Move/create web admin:
  - `SellerTerminalAdminController`
  - `CreateSellerTerminalRequest`
  - `UpdateSellerTerminalRequest`
  - `BlockSellerTerminalRequest`
  - `ResetPinRequest`
- [x] Move/create web tenant:
  - `SellerTerminalMeController`
  - `CurrentOperationalContextController`

Acceptance:

- [x] `core.sellerterminal` compiles.
- [x] Public Java imports use `core.sellerterminal.api.*`.
- [x] Internal imports use `core.sellerterminal.internal.*` only inside the slice.
- [ ] No new code imports `core.terminal.*`.

## Phase 2D — Move from `core.terminal` to `core.sellerterminal`

- [ ] Move classes from `com.tchalanet.server.core.terminal.api.*` and `.internal.*` into `com.tchalanet.server.core.sellerterminal.api.*` and `.internal.*`.
  - [x] Seller-terminal API classes moved/created under `core.sellerterminal.api.*`.
  - [x] Seller-terminal domain/application/persistence/web classes moved under `core.sellerterminal.internal.*`.
  - [x] Remove temporary `core.terminal.api.*` compatibility wrappers after build verification.
- [x] Update imports in:
  - platform.identity;
  - features.cashier;
  - features.tenantadmin;
  - features.runtime/bootstrap;
  - core.sales;
  - core.limitpolicy;
  - admin sidebar providers;
  - tests.
- [x] Remove `outletId` from commands and DTOs.
- [x] Keep `addressId` as a separate optional platform/contact feature; it is not a V0 operational pivot.
- [x] Remove `outletId` from `SellerTerminalSearchCriteria`.
- [x] Remove `outlet_id` from seller-terminal persistence for V0.
- [x] Rename permissions:
  - `terminal.manage` -> `seller_terminal.manage`
  - `terminal.block` -> `seller_terminal.block`
  - `terminal.reset_pin` -> `seller_terminal.reset_access`
  - `terminal.operational_context.read` -> `seller_terminal.operational_context.read`
- [x] Prefer routes:
  - `/admin/seller-terminals`
  - `/tenant/seller-terminal/me`
  - `/tenant/seller-terminal/operational-context`
- [x] Decide temporary aliases if needed:
  - `/tenant/terminal/me`
  - `/tenant/me/operational-context`
- [x] Search for remaining old imports/usages with `rg`, not global `grep -R`:
  - `core\.terminal`
  - `TerminalId`
  - `OutletId`
  - `SalesSessionId`

Acceptance:

- [ ] Existing seller-terminal behavior works from new package.
- [ ] No active V0 code depends on old `core.terminal`.
- [x] No seller-terminal command accepts `outletId`.
- [x] No seller-terminal command accepts `salesSessionId`.

## Phase 2E — Delete old `core.terminal`

- [ ] Confirm no imports remain for `com.tchalanet.server.core.terminal`.
- [ ] Delete old `core.terminal.api`.
- [ ] Delete old `core.terminal.internal`.
- [ ] Delete old tests.
- [ ] Delete old bean/config references.
- [ ] Update Spring Modulith/ArchUnit package declarations if needed.
- [ ] Run `./mvnw -pl tchalanet-core -am verify`.
- [ ] Run `./mvnw clean verify`.

Acceptance:

- [ ] `core.terminal` no longer exists.
- [ ] Build passes.
- [ ] App starts without missing beans.
- [ ] POS login still works.

## Phase 3 — Adapt operational context

- [ ] Simplify `OperationalRequestContext` to `SellerTerminalId` + `OperationalContextSource`.
- [ ] Remove mandatory V0 facts: `TerminalId`, `OutletId`, `SalesSessionId`.
- [x] Add explicit helper `ctx.sellerTerminalIdRequired()` or `ctx.trustedSellerTerminalContextRequired()`.
- [ ] Adapt `TchContextFilter` / context factory.
- [ ] Adapt `ActorContextResolver`.
- [ ] Adapt `OperationalContextResolver`.
- [ ] Validate seller-terminal tenant ownership during context resolution.
- [ ] Validate seller-terminal active status during context resolution.
- [ ] Ensure admin/superadmin context does not accidentally get seller-terminal context unless explicit dev/admin POS mode is retained.
- [x] Update `CurrentOperationalContextController`.
- [x] Simplify `CurrentOperationalContextView` to sellerTerminalId, terminalCode, displayName, status and source.

Acceptance:

- [ ] POS request has seller-terminal context.
- [ ] Admin request does not require seller-terminal context.
- [ ] Sale without seller-terminal context is rejected.
- [ ] Outlet/session are not required in any V0 operational context.

## Phase 4 — Adapt sales and tickets

- [x] Change `SellTicketCommand` to use `SellerTerminalId`, either explicit or context-driven.
- [x] Validate active seller-terminal before accepting sales.
- [ ] Remove sales dependency on outlet, sales session, old terminal model, separate seller model and offline grant.
- [ ] Add `seller_terminal_id` to ticket persistence.
- [ ] Add ticket index `idx_ticket_seller_terminal_sold_at` on `(tenant_id, seller_terminal_id, sold_at desc)`.
- [ ] Snapshot sale facts:
  - seller terminal code;
  - seller terminal display name;
  - commission rate;
  - odds;
  - limit facts.
- [ ] Keep `POST /tenant/tickets` idempotency.
- [ ] Ensure idempotency request hash no longer includes outlet/session.
- [ ] Adapt `TicketSoldEvent` to carry `SellerTerminalId`.
- [ ] Adapt ticket list/detail views.
- [ ] Adapt print/receipt payloads to show seller-terminal code/name.

Acceptance:

- [ ] Active seller-terminal can sell.
- [ ] Blocked seller-terminal cannot sell.
- [ ] Disabled seller-terminal cannot sell.
- [ ] Ticket row references `seller_terminal_id`.
- [ ] Ticket response displays seller-terminal facts.
- [ ] Replay with same idempotency key returns same ticket.

## Phase 5 — Reports and dashboard

- [ ] Replace outlet/session reports with seller-terminal reports.
- [ ] Add report aggregations:
  - sales by seller terminal;
  - sales by draw;
  - sales by day;
  - sales by period;
  - recent tickets by seller terminal;
  - active seller terminals;
  - blocked seller terminals;
  - commission by seller terminal.
- [ ] Update tenant admin dashboard widgets:
  - seller terminals active;
  - seller terminals blocked;
  - sales today;
  - sales by draw;
  - top seller terminals;
  - recent tickets.
- [ ] Update runtime/sidebar to point to:
  - `/admin/seller-terminals`
  - `/admin/reports/seller-terminals`
  - `/admin/controls/limits`
  - `/admin/controls/odds`
  - `/admin/promotions`
  - `/admin/draws-results`
- [x] Remove or hide menu entries for outlets, sessions, payouts, autonomy, offline sync and ledger.

Acceptance:

- [ ] Tenant admin sees seller-terminal surfaces.
- [x] No outlet/session report appears in V0 UI.
- [ ] Reports can aggregate by seller-terminal.

## Phase 6 — Park or remove V0-dead slices

- [x] Outlet: disable/remove `OutletAdminController` from runtime.
- [ ] Outlet: remove outlet from seller-terminal.
- [ ] Outlet: remove outlet from operational context.
- [x] Outlet: remove outlet from reports.
- [ ] Outlet: keep DB table only if already migrated and expensive to drop now.
- [x] Session: disable session controllers.
- [x] Session: disable session schedulers/jobs.
- [ ] Session: remove session validation from sales.
- [ ] Session: remove `SalesSessionId` from operational context.
- [x] Old terminal/seller: identify old terminal technical classes.
- [x] Old terminal/seller: delete or migrate to `sellerterminal`.
- [ ] Old terminal/seller: ensure `SellerTerminalId` is the only V0 operational ID.
- [x] Payout: park controllers/services/workflows and remove menu/permissions from V0 flows.
- [x] Ledger: park projectors, menu/report entries and ledger-specific events unless still required for audit.
- [x] Offline sync: park controllers/jobs/listeners and permissions.
- [x] Autonomy: park controllers/jobs/listeners and permissions.

Acceptance:

- [ ] App starts without parked slice beans required.
- [x] No V0 menu points to parked slices.
- [x] No V0 flow publishes or consumes parked events.
- [ ] No sale requires outlet/session/offline/autonomy.

## Phase 7 — Permissions, batch and runtime

- [ ] Add seller-terminal permissions:
  - `seller_terminal.read`
  - `seller_terminal.manage`
  - `seller_terminal.block`
  - `seller_terminal.reset_access`
  - `seller_terminal.commission.update`
  - `seller_terminal.limits.update`
  - `seller_terminal.odds.update`
  - `seller_terminal.operational_context.read`
- [x] Replace old permissions in V0 code:
  - `terminal.manage`
  - `terminal.block`
  - `terminal.reset_pin`
  - `terminal.operational_context.read`
- [x] Remove from V0 active flows:
  - `outlet.manage`
  - `session.open`
  - `session.close`
  - `payout.approve`
  - `payout.reject`
  - `offline.sync`
  - `offline.grant`
  - `autonomy.approve`
  - `autonomy.reject`
- [ ] Keep active scheduler jobs:
  - `draw.generate`
  - `draw.open_today`
  - `draw.processing.close`
  - `drawresult.fetch`
  - `drawresult.apply`
- [x] Park settlement/payout/ledger/offline/session/autonomy jobs.
- [ ] Update runtime/sidebar/page model providers.
- [ ] Update public/private bootstrap if it exposes operational state.

Acceptance:

- [ ] Permissions map to seller-terminal flows.
- [ ] Seller-terminal actor cannot access admin routes.
- [ ] Tenant admin can manage seller terminals.
- [ ] Super admin can use tenant override where allowed and audited.
- [ ] Parked jobs do not run.

## Phase 8 — Events

- [ ] Add/confirm seller-terminal events:
  - `SellerTerminalCreatedEvent`
  - `SellerTerminalUpdatedEvent`
  - `SellerTerminalBlockedEvent`
  - `SellerTerminalUnblockedEvent`
  - `SellerTerminalDisabledEvent`
  - `SellerTerminalAccessResetEvent`
- [ ] Publish events after commit only.
- [ ] Keep listeners thin.
- [ ] Do not publish events that have no V0 consumer unless useful for audit/projections.
- [ ] Stop V0 publishing parked events:
  - `SessionOpenedEvent`
  - `SessionClosedEvent`
  - `PayoutRequestedEvent`
  - `PayoutApprovedEvent`
  - `LedgerEntryCreatedEvent`
  - `OfflineSyncAcceptedEvent`
  - `AutonomyApprovedEvent`

Acceptance:

- [ ] Seller-terminal state changes publish after commit.
- [ ] Parked events are not part of active V0 path.
- [ ] Event consumers are idempotent where retained.

## Phase 9 — Database and Flyway

- [ ] Confirm migration strategy before destructive DB changes.
- [ ] Create/adjust `seller_terminal` table.
- [ ] Remove `outlet_id` from `seller_terminal` V0 model.
- [ ] Remove `address_id` from `seller_terminal` V0 model unless address stays optional platform data.
- [ ] Create/adjust `seller_terminal_external_identity` if identity mapping is DB-owned by sellerterminal.
- [ ] Add RLS policies for `seller_terminal`.
- [ ] Add indexes:
  - unique `uq_seller_terminal_code` on `(tenant_id, terminal_code)` where `deleted_at is null`;
  - `idx_seller_terminal_status` on `(tenant_id, status)` where `deleted_at is null`.
- [ ] Add `seller_terminal_id` to `ticket`.
- [ ] Add ticket seller-terminal reporting index.
- [ ] Decide whether old ticket columns become nullable first or are dropped immediately:
  - `outlet_id`
  - `sales_session_id`
  - `old_terminal_id`
  - `seller_id`
- [ ] Prefer progressive migration:
  - migration 1: add `seller_terminal_id` + backfill/dev support;
  - migration 2: make `seller_terminal_id` required for new sales;
  - migration 3: drop old columns after verification.

Acceptance:

- [ ] Flyway migrate succeeds.
- [ ] RLS isolates seller-terminal rows by tenant.
- [ ] Ticket persistence uses `seller_terminal_id`.
- [ ] Old columns are not required by V0 code.

## Phase 10 — Tests and verification

Unit tests:

- [ ] `SellerTerminal` create valid.
- [ ] `SellerTerminal` block.
- [ ] `SellerTerminal` unblock.
- [ ] `SellerTerminal` disable.
- [ ] `SellerTerminal` reset access.
- [ ] `SellerTerminal` cannot sell when blocked.

Handler tests:

- [ ] `CreateSellerTerminalHandler`.
- [ ] `UpdateSellerTerminalHandler`.
- [ ] `BlockSellerTerminalHandler`.
- [ ] `UnblockSellerTerminalHandler`.
- [ ] `DisableSellerTerminalHandler`.
- [ ] `ResetSellerTerminalAccessHandler`.

Login/context tests:

- [ ] POS token linked to active seller-terminal => context OK.
- [ ] POS token not linked => 403.
- [ ] POS token linked to blocked seller-terminal => 403.
- [ ] Admin token linked to app user => context OK.
- [ ] POS request does not fallback to app user.
- [ ] Admin request does not fallback to seller-terminal.

Web tests:

- [ ] `GET /admin/seller-terminals` paginated.
- [ ] `POST /admin/seller-terminals` without outlet/address.
- [ ] `PATCH /admin/seller-terminals/{id}/block`.
- [ ] `PATCH /admin/seller-terminals/{id}/reset-access`.
- [ ] `GET /tenant/seller-terminal/me` as seller-terminal actor.
- [ ] `GET /tenant/seller-terminal/me` as admin rejected unless alias/dev mode explicitly allows it.

Sales tests:

- [ ] Sell ticket with active seller-terminal => accepted.
- [ ] Sell ticket with blocked seller-terminal => rejected.
- [ ] Sell ticket without seller-terminal context => rejected.
- [ ] Sell ticket with seller-terminal from another tenant => rejected.
- [ ] Idempotency replay returns same ticket.

RLS tests:

- [ ] Tenant A admin cannot list tenant B seller terminals.
- [ ] Tenant A seller-terminal cannot access tenant B tickets.
- [ ] Super admin override remains auditable.

Architecture tests:

- [ ] No controller imports persistence.
- [ ] No sales import from `core.sellerterminal.internal.*`.
- [ ] No features import from `core.*.internal.*`.
- [ ] No platform import from `core.*`.
- [ ] No raw UUID outside persistence.
- [ ] ArchUnit passes.

Maven verification:

- [ ] `./mvnw -pl tchalanet-core -am verify`
- [ ] `./mvnw -pl tchalanet-platform -am verify`
- [ ] `./mvnw -pl tchalanet-features -am verify`
- [ ] `./mvnw clean verify`

Acceptance:

- [ ] All focused module checks pass.
- [ ] Full build passes.
- [ ] Application starts.
- [ ] POS login works.
- [ ] Seller-terminal sale works.
- [ ] Admin seller-terminal management works.

## Immediate implementation order

- [ ] Backup branch + OpenSpec.
- [ ] Document login flow and parking lot decision.
- [ ] Move identity adapter out of `features.cashier`.
- [ ] Create `platform.identity.api.SellerTerminalIdentityLookup` SPI.
- [ ] Create `core.sellerterminal` package.
- [ ] Move seller-terminal code from `core.terminal` to `core.sellerterminal`.
- [ ] Keep POS login tests green.
- [ ] Remove outlet/address from seller-terminal DTOs/commands/entities.
- [ ] Adapt operational context.
- [ ] Adapt sales/tickets.
- [ ] Park dead slices.
- [ ] Update runtime/sidebar/reports.
- [ ] Delete old `core.terminal`.
- [ ] Full verification.
