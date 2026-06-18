# V0 Seller Terminal Simplification — Tasks & Migration Plan

## Status

Proposed — à valider avant implémentation destructive.

## Goal

Simplifier la V0 Tchalanet autour d'une seule unité opérationnelle : `seller_terminal`.

Le `seller_terminal` remplace pour la V0 :

- `seller` séparé ;
- `terminal` technique séparé ;
- `outlet` ;
- `sales session` ;
- `offline sync` ;
- `autonomy` ;
- `payout` / `ledger` avancé.

La V0 devient :

```text
Tenant
  └── SellerTerminal
        ├── authentification POS
        ├── vente de tickets
        ├── blocage / suspension / désactivation
        ├── commission
        ├── limites
        ├── odds / barèmes override
        └── reporting
```

## Core decisions

1. `seller_terminal` est l'acteur opérationnel POS de la V0.
2. `seller_terminal` est distinct des acteurs `TENANT_ADMIN` et `SUPER_ADMIN`.
3. L'authentification seller-terminal passe par un flow séparé du flow admin.
4. `outlet` est retiré de la V0.
5. `session` est retiré de la V0.
6. L'ancien package `core.terminal` doit être migré vers une vraie slice `core.sellerterminal`.
7. L'ancien `core.terminal` sera supprimé après migration et vérification.
8. Les slices `payout`, `ledger`, `offline sync`, `autonomy` sont parking lot V1+.

---

# Phase 0 — Safety and decision record

## Tasks

- [ ] Créer une branche backup avant toute suppression destructive.

```bash
git checkout main
git pull
git checkout -b backup/pre-v0-seller-terminal-simplification
git push origin backup/pre-v0-seller-terminal-simplification
```

- [ ] Créer la branche de travail.

```bash
git checkout main
git checkout -b refactor/v0-seller-terminal-simplification
```

- [ ] Créer l'OpenSpec change :

```text
v0-seller-terminal-simplification
```

- [ ] Ajouter un decision record :

```text
ADR — V0 SellerTerminal as operational POS actor
```

- [ ] Marquer explicitement en parking lot V1+ :

```text
outlet
session
payout
ledger
offline sync
autonomy
old terminal model
separate seller model
```

- [ ] Décider si un alias temporaire `/tenant/terminal/me` reste actif pendant la migration.
- [ ] Geler tout nouveau développement V0 sur payout/offline/session/autonomy.

## Acceptance criteria

- [ ] Branche backup disponible sur remote.
- [ ] Branche refactor créée.
- [ ] OpenSpec créé.
- [ ] Parking lot documenté.
- [ ] Aucun nouveau développement ne cible les slices retirées de la V0.

---

# Phase 1 — Documentation

## Tasks

- [ ] Ajouter :

```text
docs/architecture/v0-seller-terminal-simplification.md
```

- [ ] Mettre à jour `docs/ARCHITECTURE.md`.

Remplacer les surfaces V0 actives :

```text
Tenant admin outlets
Tenant admin terminals
Tenant admin autonomy
Tenant admin sessions
Tenant admin payouts
```

par :

```text
Tenant admin seller terminals
Tenant admin limits
Tenant admin odds/barèmes
Tenant admin commissions
Tenant admin reports by seller terminal
```

- [ ] Mettre à jour `docs/PLAYBOOK.md`.

Remplacer les exemples basés sur :

```text
TerminalId
OutletId
SalesSessionId
```

par :

```text
SellerTerminalId
```

- [ ] Mettre à jour `docs/modules/core.md`.

Ajouter :

```text
core.sellerterminal
  Owner of seller_terminal operational POS actor in V0.
```

- [ ] Mettre à jour `docs/conventions/request_context_usage.md`.
- [ ] Mettre à jour `docs/conventions/user-contexte-operational.md` si présent.

Ancien contexte :

```java
public record OperationalRequestContext(
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    OperationalContextSource source
) {}
```

Nouveau contexte V0 :

```java
public record OperationalRequestContext(
    SellerTerminalId sellerTerminalId,
    OperationalContextSource source
) {}
```

- [ ] Mettre à jour `docs/conventions/inter_domain_calls.md`.

Clarifier :

```text
sales -> sellerterminal read boundary
sales must not import sellerterminal internal persistence
sales uses sellerterminal api/query/port or context snapshot
```

- [ ] Mettre à jour `docs/conventions/security_permissions.md`.

Ajouter permissions V0 :

```text
seller_terminal.read
seller_terminal.manage
seller_terminal.block
seller_terminal.reset_access
seller_terminal.commission.update
seller_terminal.limits.update
seller_terminal.odds.update
seller_terminal.operational_context.read

ticket.sell
ticket.read_own
ticket.void_own

report.seller_terminal.read
report.seller_terminal.read_own
```

- [ ] Mettre à jour `docs/conventions/web_api.md`.

Documenter endpoints admin :

```text
GET    /admin/seller-terminals
POST   /admin/seller-terminals
GET    /admin/seller-terminals/{id}
PUT    /admin/seller-terminals/{id}
PATCH  /admin/seller-terminals/{id}/block
PATCH  /admin/seller-terminals/{id}/unblock
PATCH  /admin/seller-terminals/{id}/disable
PATCH  /admin/seller-terminals/{id}/reset-access
```

Documenter endpoints POS :

```text
GET /tenant/seller-terminal/me
GET /tenant/seller-terminal/operational-context
```

Alias temporaire possible :

```text
GET /tenant/terminal/me
GET /tenant/me/operational-context
```

- [ ] Mettre à jour `docs/conventions/batch.md`.

Actif V0 :

```text
draw.generate
draw.open_today
draw.processing.close
drawresult.fetch
drawresult.apply
```

Parking lot V1+ :

```text
session cleanup
payout workflows
ledger projectors
offline sync jobs
autonomy jobs
```

- [ ] Mettre à jour `docs/conventions/idempotency.md`.

Clarifier :

```text
POST /tenant/tickets keeps Idempotency-Key.
offline retry semantics are parked V1+.
```

## Acceptance criteria

- [ ] La doc ne présente plus outlet/session/payout/offline/autonomy comme flows V0 actifs.
- [ ] La doc présente `seller_terminal` comme pivot V0.
- [ ] Les exemples backend utilisent `SellerTerminalId`.
- [ ] Les endpoints V0 sont documentés.
- [ ] Le parking lot est clair.

---

# Phase 2A — Audit seller-terminal login flow

## Current flow to document

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

## Tasks

- [ ] Documenter le flow réel du login seller-terminal.
- [ ] Vérifier `ExpectedActorTypeResolver`.
- [ ] Vérifier que `X-Tch-Client-Type = POS` force `TchActorType.SELLER_TERMINAL`.
- [ ] Vérifier qu'il n'y a aucun fallback entre POS et AppUser.

Expected behavior :

```text
POS sans seller-terminal mapping => 403 terminal.external_identity_not_linked
Admin sans app user mapping => 403 external_identity.not_linked
POS ne tente pas app user
Admin ne tente pas seller-terminal
```

- [ ] Vérifier erreurs attendues :

```text
terminal.external_identity_not_linked
terminal.not_active
external_identity.not_linked
user.not_active
external_identity.missing_verified_identity
```

- [ ] Vérifier mapping status :

```text
ACTIVE => allowed
BLOCKED => forbidden
SUSPENDED => forbidden
DISABLED => forbidden
DELETED => forbidden
```

- [ ] Vérifier que `BootstrappedActor.sellerTerminal(...)` porte :

```text
sellerTerminalId
tenantId
provider
issuer
subject
actorType = SELLER_TERMINAL
```

- [ ] Vérifier que `TchContextFilter` produit :

```text
TchRequestContext.actorType = SELLER_TERMINAL
TchRequestContext.sellerTerminalId = non-null
TchRequestContext.tenantId = seller terminal tenant
Authority = ACTOR_SELLER_TERMINAL
```

- [ ] Vérifier que les endpoints POS sont protégés par :

```java
@PreAuthorize("hasAuthority('ACTOR_SELLER_TERMINAL')")
```

## Tests

- [ ] POS token linked to active seller-terminal => context OK.
- [ ] POS token not linked => 403 `terminal.external_identity_not_linked`.
- [ ] POS token linked to blocked terminal => 403 `terminal.not_active`.
- [ ] Admin token linked to app user => context OK.
- [ ] Admin token not linked => 403 `external_identity.not_linked`.
- [ ] POS token never falls back to app user.
- [ ] Admin token never falls back to seller-terminal.

---

# Phase 2B — Move identity adapter out of features

## Problem

Current adapter is misplaced :

```text
features.cashier.sellerterminal.identity.SellerTerminalExternalIdentityAdapter
```

It imports core terminal internal persistence :

```text
core.terminal.internal.infra.persistence.sellerterminal.SellerTerminalJpaRepository
```

This is wrong because :

```text
features must stay leaf/BFF
features must not implement identity bootstrap adapters
features must not import core internal persistence
platform.identity must not depend on features
```

## Target option for V0

Use a public SPI in `platform.identity.api`, implemented by the owning core slice.

```text
platform.identity.api.SellerTerminalIdentityLookup
        ↑ implemented by
core.sellerterminal.internal.infra.identity.CoreSellerTerminalIdentityLookupAdapter
        ↑ uses
core.sellerterminal.internal.infra.persistence.SellerTerminalJpaRepository
```

## Tasks

- [ ] Create `platform.identity.api.SellerTerminalIdentityLookup`.
- [ ] Create `platform.identity.api.model.SellerTerminalIdentityBootstrapView`.
- [ ] Create `platform.identity.api.model.SellerTerminalBootstrapStatus`.
- [ ] Modify `UserBootstrapFilterImpl` to inject `SellerTerminalIdentityLookup`.
- [ ] Remove dependency on `SellerTerminalExternalIdentityPort` if possible.
- [ ] Move adapter from `features.cashier` to `core.sellerterminal.internal.infra.identity`.
- [ ] Rename adapter to `CoreSellerTerminalIdentityLookupAdapter`.
- [ ] Remove old misplaced adapter from `features.cashier`.
- [ ] Add package review/ArchUnit rule if possible :

```text
features.. must not implement platform.identity internal ports
features.. must not import core..internal..
platform.. must not import core..
```

## Acceptance criteria

- [ ] No identity bootstrap bean lives in `features.cashier`.
- [ ] `features.cashier` does not import `core.*.internal.*`.
- [ ] `UserBootstrapFilterImpl` depends on public `platform.identity.api` SPI.
- [ ] Seller-terminal lookup implementation lives in owning core slice.

---

# Phase 2C — Create `core.sellerterminal` slice

## Target structure

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

## Tasks

- [ ] Create package `com.tchalanet.server.core.sellerterminal`.
- [ ] Move/create API models :

```text
SellerTerminalStatus
SellerTerminalView
SellerTerminalSummaryRow
SellerTerminalSearchCriteria
CurrentOperationalContextView
```

- [ ] Move/create commands :

```text
CreateSellerTerminalCommand
UpdateSellerTerminalCommand
BlockSellerTerminalCommand
UnblockSellerTerminalCommand
DisableSellerTerminalCommand
ResetSellerTerminalAccessCommand
UpdateSellerTerminalCommissionCommand
UpdateSellerTerminalLimitsCommand
UpdateSellerTerminalOddsCommand
```

- [ ] Move/create queries :

```text
GetSellerTerminalQuery
GetSellerTerminalMeQuery
ListSellerTerminalsQuery
GetCurrentOperationalContextQuery
```

- [ ] Move/create events :

```text
SellerTerminalCreatedEvent
SellerTerminalUpdatedEvent
SellerTerminalBlockedEvent
SellerTerminalUnblockedEvent
SellerTerminalDisabledEvent
SellerTerminalAccessResetEvent
```

- [ ] Move/create domain :

```text
SellerTerminal
SellerTerminalAccess
SellerTerminalCommission
SellerTerminalStatus policy
```

- [ ] Move/create persistence :

```text
SellerTerminalJpaEntity
SellerTerminalJpaRepository
SellerTerminalJpaMapper
SellerTerminalJpaAdapter
```

- [ ] Move/create identity adapter :

```text
CoreSellerTerminalIdentityLookupAdapter
```

- [ ] Move/create web admin :

```text
SellerTerminalAdminController
CreateSellerTerminalRequest
UpdateSellerTerminalRequest
BlockSellerTerminalRequest
ResetPinRequest
```

- [ ] Move/create web tenant :

```text
SellerTerminalMeController
CurrentOperationalContextController
```

## Acceptance criteria

- [ ] `core.sellerterminal` compiles.
- [ ] Public Java imports use `core.sellerterminal.api.*`.
- [ ] Internal imports use `core.sellerterminal.internal.*` only inside the slice.
- [ ] No new code imports `core.terminal.*`.

---

# Phase 2D — Move from `core.terminal` to `core.sellerterminal`

## Tasks

- [ ] Move classes from :

```text
com.tchalanet.server.core.terminal.api.*
com.tchalanet.server.core.terminal.internal.*
```

into :

```text
com.tchalanet.server.core.sellerterminal.api.*
com.tchalanet.server.core.sellerterminal.internal.*
```

- [ ] Update imports in :

```text
platform.identity
features.cashier
features.tenantadmin
features.runtime/bootstrap
core.sales
core.limitpolicy
admin sidebar providers
tests
```

- [ ] Remove `outletId` from commands and DTOs.
- [ ] Remove `addressId` from commands and DTOs unless address remains a separate optional platform feature.
- [ ] Remove `outletId` from `SellerTerminalSearchCriteria`.
- [ ] Remove `outlet_id` from seller-terminal persistence for V0.
- [ ] Rename permissions :

```text
terminal.manage -> seller_terminal.manage
terminal.block -> seller_terminal.block
terminal.reset_pin -> seller_terminal.reset_access
terminal.operational_context.read -> seller_terminal.operational_context.read
```

- [ ] Decide routes :

Preferred target :

```text
/admin/seller-terminals
/tenant/seller-terminal/me
/tenant/seller-terminal/operational-context
```

Temporary aliases if needed :

```text
/tenant/terminal/me
/tenant/me/operational-context
```

- [ ] Search for remaining old imports :

```bash
grep -R "core\.terminal" -n tchalanet-server/
grep -R "TerminalId" -n tchalanet-server/
grep -R "OutletId" -n tchalanet-server/
grep -R "SalesSessionId" -n tchalanet-server/
```

## Acceptance criteria

- [ ] Existing seller-terminal behavior works from new package.
- [ ] No active V0 code depends on old `core.terminal`.
- [ ] No seller-terminal command accepts `outletId`.
- [ ] No seller-terminal command accepts `salesSessionId`.

---

# Phase 2E — Delete old `core.terminal`

## Tasks

- [ ] Confirm no imports remain :

```bash
grep -R "com.tchalanet.server.core.terminal" -n tchalanet-server/
```

- [ ] Delete old packages :

```text
core.terminal.api
core.terminal.internal
```

- [ ] Delete old tests.
- [ ] Delete old bean/config references.
- [ ] Update Spring Modulith/ArchUnit package declarations if needed.
- [ ] Run focused verification :

```bash
./mvnw -pl tchalanet-core -am verify
```

- [ ] Run full verification :

```bash
./mvnw clean verify
```

## Acceptance criteria

- [ ] `core.terminal` no longer exists.
- [ ] Build passes.
- [ ] App starts without missing beans.
- [ ] Login POS still works.

---

# Phase 3 — Adapt operational context

## Tasks

- [ ] Simplify `OperationalRequestContext` to :

```java
public record OperationalRequestContext(
    SellerTerminalId sellerTerminalId,
    OperationalContextSource source
) {}
```

- [ ] Remove mandatory facts from V0 context :

```text
TerminalId
OutletId
SalesSessionId
```

- [ ] Add explicit helper :

```java
ctx.sellerTerminalIdRequired()
```

or :

```java
ctx.trustedSellerTerminalContextRequired()
```

- [ ] Adapt `TchContextFilter` / context factory.
- [ ] Adapt `ActorContextResolver`.
- [ ] Adapt `OperationalContextResolver`.
- [ ] Validate seller-terminal tenant ownership during context resolution.
- [ ] Validate seller-terminal active status during context resolution.
- [ ] Ensure admin/superadmin context does not accidentally get seller-terminal context unless explicit dev/admin POS mode is retained.
- [ ] Update `CurrentOperationalContextController`.

Before :

```java
new GetCurrentOperationalContextQuery(
    ctx.effectiveTenantIdRequired(),
    ctx.currentUserIdRequired(),
    ctx.operationalContext()
)
```

After V0 target :

```java
new GetCurrentOperationalContextQuery(
    ctx.effectiveTenantIdRequired(),
    ctx.sellerTerminalIdRequired()
)
```

- [ ] Simplify `CurrentOperationalContextView` :

```java
public record CurrentOperationalContextView(
    SellerTerminalId sellerTerminalId,
    String terminalCode,
    String displayName,
    SellerTerminalStatus status,
    OperationalContextSource source
) {}
```

## Acceptance criteria

- [ ] POS request has seller-terminal context.
- [ ] Admin request does not require seller-terminal context.
- [ ] Sale without seller-terminal context is rejected.
- [ ] Outlet/session are not required in any V0 operational context.

---

# Phase 4 — Adapt sales and tickets

## Tasks

- [ ] Change `SellTicketCommand` to use `SellerTerminalId`.

Option explicit :

```java
public record SellTicketCommand(
    TenantId tenantId,
    SellerTerminalId sellerTerminalId,
    List<SaleLineInput> lines,
    ...
) {}
```

Option context-driven :

```java
public record SellTicketCommand(
    TenantId tenantId,
    List<SaleLineInput> lines,
    ...
) {}
```

and handler reads seller-terminal from context.

- [ ] Validate active seller-terminal before accepting sales.
- [ ] Remove sales dependency on :

```text
outlet
sales session
old terminal model
separate seller model
offline grant
```

- [ ] Add `seller_terminal_id` to ticket persistence.
- [ ] Add ticket indexes :

```sql
create index idx_ticket_seller_terminal_sold_at
on ticket (tenant_id, seller_terminal_id, sold_at desc);
```

- [ ] Snapshot sale facts :

```text
seller_terminal_code_snapshot
seller_terminal_display_name_snapshot
commission_rate_snapshot
odds_snapshot
limit_snapshot
```

- [ ] Keep `POST /tenant/tickets` idempotency.
- [ ] Ensure idempotency request hash no longer includes outlet/session.
- [ ] Adapt `TicketSoldEvent` to carry `SellerTerminalId`.
- [ ] Adapt ticket list/detail views.
- [ ] Adapt print/receipt payloads to show seller-terminal code/name.

## Acceptance criteria

- [ ] Active seller-terminal can sell.
- [ ] Blocked seller-terminal cannot sell.
- [ ] Disabled seller-terminal cannot sell.
- [ ] Ticket row references `seller_terminal_id`.
- [ ] Ticket response displays seller-terminal facts.
- [ ] Replay with same idempotency key returns same ticket.

---

# Phase 5 — Reports and dashboard

## Tasks

- [ ] Replace outlet/session reports with seller-terminal reports.
- [ ] Add report aggregations :

```text
sales by seller_terminal
sales by draw
sales by day
sales by period
recent tickets by seller_terminal
active seller terminals
blocked seller terminals
commission by seller_terminal
```

- [ ] Update tenant admin dashboard widgets :

```text
Seller terminals active
Seller terminals blocked
Sales today
Sales by draw
Top seller terminals
Recent tickets
```

- [ ] Update runtime/sidebar to point to :

```text
/admin/seller-terminals
/admin/reports/seller-terminals
/admin/controls/limits
/admin/controls/odds
/admin/promotions
/admin/draws-results
```

- [ ] Remove or hide menu entries :

```text
Outlets
Sessions
Payouts
Autonomy
Offline sync
Ledger
```

## Acceptance criteria

- [ ] Tenant admin sees seller-terminal surfaces.
- [ ] No outlet/session report appears in V0 UI.
- [ ] Reports can aggregate by seller-terminal.

---

# Phase 6 — Park or remove V0-dead slices

## Outlet

- [ ] Disable or remove `OutletAdminController` from runtime.
- [ ] Remove outlet from seller-terminal.
- [ ] Remove outlet from operational context.
- [ ] Remove outlet from reports.
- [ ] Keep DB table only if already migrated and expensive to drop now.

## Session

- [ ] Disable session controllers.
- [ ] Disable session schedulers/jobs.
- [ ] Remove session validation from sales.
- [ ] Remove `SalesSessionId` from operational context.

## Old terminal / seller

- [ ] Identify old terminal technical classes.
- [ ] Delete or migrate to `sellerterminal`.
- [ ] Ensure `SellerTerminalId` is the only V0 operational ID.

## Payout

- [ ] Park payout controllers.
- [ ] Park payout services/workflows.
- [ ] Remove payout menu entries.
- [ ] Remove payout permissions from V0 flows.

## Ledger

- [ ] Park ledger projectors.
- [ ] Stop publishing ledger-specific events from V0 flows unless still required for audit.
- [ ] Remove ledger menu/report entries.

## Offline sync

- [ ] Park offline sync controllers.
- [ ] Park offline sync jobs.
- [ ] Park offline sync listeners.
- [ ] Remove offline permissions.

## Autonomy

- [ ] Park autonomy controllers.
- [ ] Park autonomy jobs.
- [ ] Park autonomy listeners.
- [ ] Remove autonomy permissions.

## Acceptance criteria

- [ ] App starts without parked slice beans required.
- [ ] No V0 menu points to parked slices.
- [ ] No V0 flow publishes or consumes parked events.
- [ ] No sale requires outlet/session/offline/autonomy.

---

# Phase 7 — Permissions, batch and runtime

## Tasks

- [ ] Add seller-terminal permission mappings :

```text
seller_terminal.read
seller_terminal.manage
seller_terminal.block
seller_terminal.reset_access
seller_terminal.commission.update
seller_terminal.limits.update
seller_terminal.odds.update
seller_terminal.operational_context.read
```

- [ ] Replace old permissions in V0 code :

```text
terminal.manage
terminal.block
terminal.reset_pin
terminal.operational_context.read
```

- [ ] Remove from V0 active flows :

```text
outlet.manage
session.open
session.close
payout.approve
payout.reject
offline.sync
offline.grant
autonomy.approve
autonomy.reject
```

- [ ] Keep active scheduler jobs :

```text
draw.generate
draw.open_today
draw.processing.close
drawresult.fetch
drawresult.apply
```

- [ ] Park jobs :

```text
settlement/payout
ledger
offline
session
autonomy
```

- [ ] Update runtime/sidebar/page model providers.
- [ ] Update public/private bootstrap if it exposes operational state.

## Acceptance criteria

- [ ] Permissions map to seller-terminal flows.
- [ ] Seller-terminal actor cannot access admin routes.
- [ ] Tenant admin can manage seller terminals.
- [ ] Super admin can use tenant override where allowed and audited.
- [ ] Parked jobs do not run.

---

# Phase 8 — Events

## Tasks

- [ ] Add/confirm active seller-terminal events :

```text
SellerTerminalCreatedEvent
SellerTerminalUpdatedEvent
SellerTerminalBlockedEvent
SellerTerminalUnblockedEvent
SellerTerminalDisabledEvent
SellerTerminalAccessResetEvent
```

- [ ] Publish events after commit only.
- [ ] Keep listeners thin.
- [ ] Do not publish events that have no V0 consumer unless useful for audit/projections.
- [ ] Stop V0 publishing parked events :

```text
SessionOpenedEvent
SessionClosedEvent
PayoutRequestedEvent
PayoutApprovedEvent
LedgerEntryCreatedEvent
OfflineSyncAcceptedEvent
AutonomyApprovedEvent
```

## Acceptance criteria

- [ ] Seller-terminal state changes publish after commit.
- [ ] Parked events are not part of active V0 path.
- [ ] Event consumers are idempotent where retained.

---

# Phase 9 — Database and Flyway

## Tasks

- [ ] Confirm migration strategy before destructive DB changes.
- [ ] Create/adjust `seller_terminal` table.
- [ ] Remove `outlet_id` from `seller_terminal` V0 model.
- [ ] Remove `address_id` from `seller_terminal` V0 model unless address stays optional platform data.
- [ ] Create/adjust `seller_terminal_external_identity` if identity mapping is DB-owned by sellerterminal.
- [ ] Add RLS policies for `seller_terminal`.
- [ ] Add indexes :

```sql
create unique index uq_seller_terminal_code
on seller_terminal (tenant_id, terminal_code)
where deleted_at is null;

create index idx_seller_terminal_status
on seller_terminal (tenant_id, status)
where deleted_at is null;
```

- [ ] Add `seller_terminal_id` to `ticket`.
- [ ] Add ticket seller-terminal reporting index.
- [ ] Decide whether old columns become nullable first or are dropped immediately :

```text
outlet_id
sales_session_id
old_terminal_id
seller_id
```

Recommended approach :

```text
Migration 1: add seller_terminal_id + backfill/dev support
Migration 2: make seller_terminal_id required for new sales
Migration 3: drop old columns after verification
```

## Acceptance criteria

- [ ] Flyway migrate succeeds.
- [ ] RLS isolates seller-terminal rows by tenant.
- [ ] Ticket persistence uses `seller_terminal_id`.
- [ ] Old columns are not required by V0 code.

---

# Phase 10 — Tests and verification

## Unit tests

- [ ] `SellerTerminal` create valid.
- [ ] `SellerTerminal` block.
- [ ] `SellerTerminal` unblock.
- [ ] `SellerTerminal` disable.
- [ ] `SellerTerminal` reset access.
- [ ] `SellerTerminal` cannot sell when blocked.

## Handler tests

- [ ] `CreateSellerTerminalHandler`.
- [ ] `UpdateSellerTerminalHandler`.
- [ ] `BlockSellerTerminalHandler`.
- [ ] `UnblockSellerTerminalHandler`.
- [ ] `DisableSellerTerminalHandler`.
- [ ] `ResetSellerTerminalAccessHandler`.

## Login/context tests

- [ ] POS token linked to active seller-terminal => context OK.
- [ ] POS token not linked => 403.
- [ ] POS token linked to blocked seller-terminal => 403.
- [ ] Admin token linked to app user => context OK.
- [ ] POS request does not fallback to app user.
- [ ] Admin request does not fallback to seller-terminal.

## Web tests

- [ ] `GET /admin/seller-terminals` paginated.
- [ ] `POST /admin/seller-terminals` without outlet/address.
- [ ] `PATCH /admin/seller-terminals/{id}/block`.
- [ ] `PATCH /admin/seller-terminals/{id}/reset-access`.
- [ ] `GET /tenant/seller-terminal/me` as seller-terminal actor.
- [ ] `GET /tenant/seller-terminal/me` as admin rejected unless alias/dev mode explicitly allows it.

## Sales tests

- [ ] Sell ticket with active seller-terminal => accepted.
- [ ] Sell ticket with blocked seller-terminal => rejected.
- [ ] Sell ticket without seller-terminal context => rejected.
- [ ] Sell ticket with seller-terminal from another tenant => rejected.
- [ ] Idempotency replay returns same ticket.

## RLS tests

- [ ] Tenant A admin cannot list tenant B seller terminals.
- [ ] Tenant A seller-terminal cannot access tenant B tickets.
- [ ] Super admin override remains auditable.

## Architecture tests

- [ ] No controller imports persistence.
- [ ] No sales import from `core.sellerterminal.internal.*`.
- [ ] No features import from `core.*.internal.*`.
- [ ] No platform import from `core.*`.
- [ ] No raw UUID outside persistence.
- [ ] ArchUnit passes.

## Maven verification

Focused :

```bash
./mvnw -pl tchalanet-core -am verify
./mvnw -pl tchalanet-platform -am verify
./mvnw -pl tchalanet-features -am verify
```

Full :

```bash
./mvnw clean verify
```

## Acceptance criteria

- [ ] All focused module checks pass.
- [ ] Full build passes.
- [ ] Application starts.
- [ ] POS login works.
- [ ] Seller-terminal sale works.
- [ ] Admin seller-terminal management works.

---

# Immediate implementation order

Recommended order to reduce risk :

```text
1. Backup branch + OpenSpec.
2. Document login flow and parking lot decision.
3. Move identity adapter out of features.cashier.
4. Create platform.identity.api SellerTerminalIdentityLookup SPI.
5. Create core.sellerterminal package.
6. Move seller-terminal code from core.terminal to core.sellerterminal.
7. Keep POS login tests green.
8. Remove outlet/address from seller-terminal DTOs/commands/entities.
9. Adapt operational context.
10. Adapt sales/tickets.
11. Park dead slices.
12. Update runtime/sidebar/reports.
13. Delete old core.terminal.
14. Full verification.
```

---

# Final V0 target summary

```text
Active V0:
- platform.identity
- platform.accesscontrol
- platform.audit
- platform.tenantconfig
- core.sellerterminal
- core.sales
- core.draw
- core.drawresult
- core.limitpolicy simplified
- core.promotion
- features.tenantadmin
- features.runtime/bootstrap
- features.reporting/dashboard

Parked V1+:
- outlet
- session
- old terminal
- separate seller
- payout
- ledger
- offline sync
- autonomy
```

