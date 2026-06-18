# Design: v0-seller-terminal-simplification

## Boundary

```text
core.sellerterminal
  -> tenant-scoped seller-terminal aggregate, POS actor state and admin/runtime operations

core.sales
  -> ticket sale validation/persistence using SellerTerminalId

common.context.operational
  -> neutral parsed seller-terminal operational context only

platform.identity
  -> authentication/bootstrap orchestration and public seller-terminal lookup SPI

features.tenantadmin
  -> BFF/orchestration only when a screen combines several domains
```

`features` MUST NOT own seller-terminal CRUD if the operation is mono-domain. CRUD belongs in `core.sellerterminal.infra.web.admin`.

`features` also MUST NOT own seller-terminal identity bootstrap adapters. Seller-terminal lookup for authentication is a public platform identity SPI implemented by the owning core slice.

## POS actor and login flow

V0 separates admin actors from seller-terminal POS actors.

Expected bootstrap flow:

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

Expected behavior:

```text
POS without seller-terminal mapping -> 403 terminal.external_identity_not_linked
Admin without app user mapping      -> 403 external_identity.not_linked
POS never falls back to AppUser
Admin never falls back to SellerTerminal
```

Expected errors include:

- `terminal.external_identity_not_linked`
- `terminal.not_active`
- `external_identity.not_linked`
- `user.not_active`
- `external_identity.missing_verified_identity`

Seller-terminal status mapping:

```text
ACTIVE    -> allowed
BLOCKED   -> forbidden
SUSPENDED -> forbidden
DISABLED  -> forbidden
DELETED   -> forbidden
```

`BootstrappedActor.sellerTerminal(...)` must carry:

- seller terminal id;
- tenant id;
- provider;
- issuer;
- subject;
- actor type `SELLER_TERMINAL`.

`TchContextFilter` must produce:

- actor type `SELLER_TERMINAL`;
- non-null seller terminal id;
- tenant id from the seller terminal;
- authority `ACTOR_SELLER_TERMINAL`.

POS endpoints are protected by seller-terminal actor authority:

```java
@PreAuthorize("hasAuthority('ACTOR_SELLER_TERMINAL')")
```

## Identity lookup ownership

Current invalid ownership to remove:

```text
features.cashier.sellerterminal.identity.SellerTerminalExternalIdentityAdapter
  -> imports core.terminal.internal.infra.persistence.sellerterminal.SellerTerminalJpaRepository
```

Target V0 ownership:

```text
platform.identity.api.SellerTerminalIdentityLookup
        ↑ implemented by
core.sellerterminal.internal.infra.identity.CoreSellerTerminalIdentityLookupAdapter
        ↑ uses
core.sellerterminal.internal.infra.persistence.SellerTerminalJpaRepository
```

Required public API types:

- `platform.identity.api.SellerTerminalIdentityLookup`
- `platform.identity.api.model.SellerTerminalIdentityBootstrapView`
- `platform.identity.api.model.SellerTerminalBootstrapStatus`

`UserBootstrapFilterImpl` should inject the public SPI, not a `features.cashier` adapter.

## Target domain

Recommended package shape:

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

`seller_terminal` represents the concrete tenant operational unit that can sell tickets.

Core fields:

```text
id
tenant_id
terminal_code
display_name
first_name
last_name
phone_number
firebase_uid
pin_hash
commission_rate
status
blocked_reason
created_at
updated_at
created_by
updated_by
deleted_at
```

Status V0:

```text
ACTIVE
BLOCKED
SUSPENDED
DISABLED
DELETED
```

Legacy `core.terminal` is not the final home for this model. Implementation should move current seller-terminal behavior from `core.terminal` into `core.sellerterminal`, then delete `core.terminal` after import and bean verification.

## Database direction

Recommended table:

```sql
seller_terminal (
  id uuid primary key,
  tenant_id uuid not null,
  terminal_code varchar(64) not null,
  display_name varchar(160) not null,
  first_name varchar(120),
  last_name varchar(120),
  phone_number varchar(40),
  firebase_uid varchar(160),
  pin_hash varchar(255),
  commission_rate numeric(5,2),
  status varchar(32) not null,
  blocked_reason text,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  created_by uuid,
  updated_by uuid,
  deleted_at timestamptz
)
```

The table is tenant-scoped and MUST follow tenant RLS/index conventions.

Recommended indexes:

```sql
create unique index uq_seller_terminal_code
on seller_terminal (tenant_id, terminal_code)
where deleted_at is null;

create index idx_seller_terminal_status
on seller_terminal (tenant_id, status)
where deleted_at is null;
```

V0 active tables:

- tenant
- identity/user mapping
- seller_terminal
- optional seller_terminal_external_identity if identity mapping is owned by sellerterminal
- ticket and ticket_line
- draw and draw_result
- promotion/campaign
- simplified limit_policy/limit_rule
- tenant_config
- audit_log
- idempotency_record
- processed_event where projectors remain active

V0 parking lot tables or slices:

- seller
- terminal
- sales_session and session_event
- payout and payout_workflow
- ledger_entry
- offline_grant and offline sync batches/items
- autonomy request/policy

Ticket migration should be progressive:

```text
Migration 1: add seller_terminal_id and backfill/dev support
Migration 2: make seller_terminal_id required for new sales
Migration 3: drop old ticket columns after verification
```

Old ticket columns to retire from V0 code:

- `outlet_id`
- `sales_session_id`
- old terminal id
- seller id

## Sales direction

V0 sale execution depends on:

- seller_terminal
- draw
- drawresult
- promotion
- simplified limitpolicy
- idempotency
- audit

`SellTicketCommand` should move from session/outlet/terminal identity to seller-terminal identity.

Explicit command option:

```java
SellTicketCommand(
  TenantId tenantId,
  SellerTerminalId sellerTerminalId,
  List<SaleLineInput> lines,
  IdempotencyKey idempotencyKey
)
```

Context-resolved option:

```java
SellTicketCommand(
  TenantId tenantId,
  List<SaleLineInput> lines,
  IdempotencyKey idempotencyKey
)
```

The handler then obtains the terminal through:

```java
ctx.trustedSellerTerminalContextRequired()
```

Tickets SHOULD reference `seller_terminal_id` and snapshot sell-time facts:

- seller terminal code;
- seller terminal display name;
- commission rate snapshot;
- odds snapshot where applicable;
- limit policy snapshot where applicable.

This preserves historical reporting when admin settings change later.

Recommended reporting index:

```sql
create index idx_ticket_seller_terminal_sold_at
on ticket (tenant_id, seller_terminal_id, sold_at desc);
```

The idempotency request hash must no longer include outlet/session once those facts are no longer part of V0 sales.

## Context direction

V0 operational context should be:

```java
OperationalRequestContext(
  SellerTerminalId sellerTerminalId,
  OperationalContextSource source
)
```

Operational context helpers should be explicit:

```java
ctx.sellerTerminalIdRequired()
```

or:

```java
ctx.trustedSellerTerminalContextRequired()
```

Fail-fast order:

1. tenant context required;
2. authenticated actor required;
3. seller terminal resolved;
4. seller terminal belongs to current tenant;
5. seller terminal is `ACTIVE`;
6. seller terminal is not blocked;
7. sale permissions pass;
8. V0 limits pass;
9. draw is open and cutoff passes;
10. sale is accepted.

Remove sell-time gates for:

- outlet existence/status;
- session existence/status;
- session terminal/outlet/seller match;
- offline grant;
- offline sync gate.

Admin and superadmin requests must not accidentally acquire seller-terminal context unless a deliberately documented dev/admin POS mode is retained.

## Permissions

Remove or disable for V0:

- `outlet.manage`
- `session.open`
- `session.close`
- `payout.approve`
- `payout.reject`
- `offline.sync`
- `offline.grant`
- `autonomy.approve`
- `autonomy.reject`
- old-model `terminal.manage`
- old-model `terminal.block`
- old-model `terminal.reset_pin`
- old-model `terminal.operational_context.read`

Add or keep V0 permissions:

- `seller_terminal.read`
- `seller_terminal.manage`
- `seller_terminal.block`
- `seller_terminal.reset_access`
- `seller_terminal.commission.update`
- `seller_terminal.limits.update`
- `seller_terminal.odds.update`
- `seller_terminal.operational_context.read`
- `ticket.sell`
- `ticket.read_own`
- `ticket.void_own`
- `report.seller_terminal.read`
- `report.seller_terminal.read_own`

Controllers must express authorization through project-standard permission annotations or `@PreAuthorize`, not manual controller logic.

## Admin and POS API direction

Replace V0-dead admin endpoints:

```text
/admin/sellers
/admin/terminals
/admin/sessions
/admin/outlets mandatory
/admin/payouts
/admin/autonomy
```

with:

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

POS endpoints:

```text
GET /tenant/seller-terminal/me
GET /tenant/seller-terminal/operational-context
```

Temporary aliases may be retained only if documented:

```text
GET /tenant/terminal/me
GET /tenant/me/operational-context
```

## Batch direction

V0 active jobs:

- draw generation;
- opening today draws;
- draw close/cutoff processing;
- result fetch;
- result application.

Parking lot:

- sales settlement;
- payout finalization;
- ledger projectors;
- offline sync jobs;
- session cleanup;
- autonomy expiration.

Schedulers remain thin and call commands/jobs. They must not own business logic.

## Events

V0 active events:

- `TicketSoldEvent`
- `TicketVoidedEvent`
- `DrawResultAppliedEvent`
- `SellerTerminalCreatedEvent`
- `SellerTerminalUpdatedEvent`
- `SellerTerminalBlockedEvent`
- `SellerTerminalUnblockedEvent`
- `SellerTerminalDisabledEvent`
- `SellerTerminalAccessResetEvent`
- `TenantConfigUpdatedEvent`

Parking lot events:

- `SessionOpenedEvent`
- `SessionClosedEvent`
- `PayoutRequestedEvent`
- `PayoutApprovedEvent`
- `LedgerEntryCreatedEvent`
- `OfflineSyncAcceptedEvent`
- `AutonomyApprovedEvent`

Cross-domain events remain after-commit and idempotently consumed.

## Limit policy

`core.limitpolicy` remains active for V0 because a bad limit decision can cause financial loss.

It no longer depends on:

- session;
- outlet;
- old terminal model;
- autonomy;
- ledger;
- offline sync.

It checks:

- tenant;
- optional seller terminal;
- draw/channel/game;
- amount;
- selection;
- period.

Future V0.1/V1 can decide whether this stays in `core.limitpolicy` or moves to a more declarative tenant/catalog rule model.

## Reports and dashboard

V0 reporting replaces outlet/session aggregates with seller-terminal aggregates:

- sales by seller terminal;
- sales by draw;
- sales by day;
- sales by period;
- recent tickets by seller terminal;
- active seller terminals;
- blocked seller terminals;
- commission by seller terminal.

Tenant admin runtime/sidebar should point to:

- `/admin/seller-terminals`
- `/admin/reports/seller-terminals`
- `/admin/controls/limits`
- `/admin/controls/odds`
- `/admin/promotions`
- `/admin/draws-results`

It should hide parked surfaces:

- outlets;
- sessions;
- payouts;
- autonomy;
- offline sync;
- ledger.
