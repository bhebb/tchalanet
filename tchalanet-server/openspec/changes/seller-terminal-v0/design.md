# Design: SellerTerminal V0

## Decision

`SellerTerminal` is the V0 model for field sellers.

It combines:

```text
seller profile
+ terminal access
+ commission
+ operational status
+ optional outlet
```

It replaces the heavy V0 path:

```text
AppUser + TenantMembership(CASHIER) + Seller + Terminal + Outlet + Session
```

with:

```text
SellerTerminal
```

## Domain

Canonical package target:

```text
core.terminal
  api/
    command/
    query/
    model/
    event/
  internal/
    domain/
    application/
    infra/
```

Suggested classes:

```text
core.terminal.api.command.CreateSellerTerminalCommand
core.terminal.api.command.UpdateSellerTerminalCommand
core.terminal.api.command.BlockSellerTerminalCommand
core.terminal.api.command.UnblockSellerTerminalCommand
core.terminal.api.command.DisableSellerTerminalCommand
core.terminal.api.command.ResetSellerTerminalAccessCommand

core.terminal.api.query.GetSellerTerminalQuery
core.terminal.api.query.ListSellerTerminalsQuery
core.terminal.api.query.GetSellerTerminalForSaleValidationQuery

core.terminal.api.model.SellerTerminalView
core.terminal.api.model.SellerTerminalSummaryRow
core.terminal.api.model.SellerTerminalStatus
```

## Aggregate / Entity

```text
SellerTerminal
- id
- tenantId
- terminalCode
- firstName
- lastName
- displayName
- phoneNumber
- addressId nullable       → FK to shared address table
- status
- commissionRate
- outletId nullable
- lastSeenAt
- activatedAt
- blockedAt
- blockedBy
- blockedReason
- disabledAt

SellerTerminalExternalIdentity  (one per terminal per provider)
- id
- sellerTerminalId
- provider                 → CHECK ('FIREBASE', ...)
- issuer
- externalSubject
```

## Status

```text
PENDING
ACTIVE
BLOCKED
DISABLED
```

Rules:

| Status | Can sell | Billable | Notes |
|---|---:|---:|---|
| PENDING | no | no | created but not ready |
| ACTIVE | yes | yes | normal selling |
| BLOCKED | no | yes | temporary control block |
| DISABLED | no | no | removed from use |

## Commission

Default:

```text
15.00%
```

Commission rule V0:

```text
commissionAmount = ticketTotalStake * commissionRate / 100
```

No V0 support for:

- commission by game;
- commission by profit;
- tiered commissions;
- payout-adjusted commissions.

Ticket sale must snapshot:

```text
seller_terminal_id
seller_commission_rate_snapshot
seller_commission_amount_snapshot
```

## External Provider Authentication

Tchalanet never issues its own JWTs for terminals. Authentication is always delegated to an external provider (Firebase in this implementation). Tchalanet owns authorization only.

Provider-neutral pattern (established by `provider-neutral-access-context-v1`):

- `AccessResolutionFilter` validates the incoming provider token.
- It resolves the `externalSubject` to a `SellerTerminal` via `(provider, issuer, externalSubject)`.
- It builds a `TchRequestContext` with actor type `SELLER_TERMINAL`.
- Business authorization (status ACTIVE, permission `terminal.sell`) is enforced by Tchalanet, not by provider claims.

When an admin creates a SellerTerminal:

1. Backend provisions a Firebase technical user.
2. Backend stores provider identity:
   - provider = `FIREBASE`
   - issuer = Firebase issuer
   - externalSubject = Firebase UID
3. SellerTerminal is linked to that external subject.
4. Flutter logs in using terminal code + PIN/password mapped to Firebase credentials.
5. `AccessResolutionFilter` verifies the Firebase token and resolves it to SellerTerminal.

When an admin resets access:

- Backend resets the Firebase user credential (new password/PIN).
- Tchalanet does not rotate any JWT — there is no Tchalanet JWT.

When an admin disables a terminal:

- Backend disables the Firebase user (prevents new tokens).
- Tchalanet also enforces `status = DISABLED` at the business layer independently.

## Database

### seller_terminal

```sql
CREATE TABLE seller_terminal (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL,

  terminal_code varchar(64) NOT NULL,
  first_name varchar(120) NULL,
  last_name varchar(120) NULL,
  display_name varchar(180) NOT NULL,
  phone_number varchar(64) NULL,

  address_line1 varchar(240) NULL,
  address_line2 varchar(240) NULL,
  city varchar(120) NULL,
  region varchar(120) NULL,
  country varchar(120) NULL,

  external_provider varchar(32) NULL,
  external_issuer varchar(240) NULL,
  external_subject varchar(240) NULL,

  status varchar(32) NOT NULL,
  commission_rate numeric(5,2) NOT NULL DEFAULT 15.00,

  odds_profile_id uuid NULL,
  limit_profile_id uuid NULL,
  outlet_id uuid NULL,

  last_seen_at timestamptz NULL,
  activated_at timestamptz NULL,
  blocked_at timestamptz NULL,
  blocked_by uuid NULL,
  blocked_reason varchar(500) NULL,
  disabled_at timestamptz NULL,

  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,

  CONSTRAINT uq_seller_terminal_code UNIQUE (tenant_id, terminal_code),
  CONSTRAINT uq_seller_terminal_external UNIQUE (external_provider, external_issuer, external_subject)
);
```

If access context introduces `seller_terminal_external_identity`, then `external_*` may be normalized into that table.

### ticket additions

```sql
ALTER TABLE ticket
  ADD COLUMN seller_terminal_id uuid NULL,
  ADD COLUMN seller_commission_rate_snapshot numeric(5,2) NULL,
  ADD COLUMN seller_commission_amount_snapshot numeric(12,2) NULL;
```

## Admin Web UX

Menu:

```text
Terminaux / Vendeurs
```

List columns:

```text
Code terminal
Nom vendeur
Téléphone
Statut
Commission
Ventes aujourd'hui
Commission aujourd'hui
Dernière vente
Actions
```

Actions:

```text
Créer
Modifier
Bloquer
Débloquer
Désactiver
Réinitialiser accès/PIN
Voir ventes
```

## APIs

Admin:

```http
GET    /admin/seller-terminals
POST   /admin/seller-terminals
GET    /admin/seller-terminals/{id}
PUT    /admin/seller-terminals/{id}
POST   /admin/seller-terminals/{id}/block
POST   /admin/seller-terminals/{id}/unblock
POST   /admin/seller-terminals/{id}/disable
POST   /admin/seller-terminals/{id}/reset-access
```

Terminal:

```http
GET /tenant/terminal/me
```

Sales integration:

```http
POST /tenant/tickets
```

## Authorization

Admin surfaces:

```java
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
```

Sensitive actions:

```java
@PreAuthorize("hasPermission('terminal.manage')")
@PreAuthorize("hasPermission('terminal.block')")
@PreAuthorize("hasPermission('terminal.reset_pin')")
```

Terminal surface:

```java
@PreAuthorize("hasAuthority('ACTOR_SELLER_TERMINAL')")
```

Sales:

```java
@PreAuthorize("hasPermission('terminal.sell')")
```

Core sales must still validate terminal ACTIVE from business state before creating ticket.

## Audit

### Business audit_event (mandatory)

```text
SELLER_TERMINAL_CREATE
SELLER_TERMINAL_UPDATE
SELLER_TERMINAL_BLOCK
SELLER_TERMINAL_UNBLOCK
SELLER_TERMINAL_DISABLE
SELLER_TERMINAL_RESET_ACCESS
SELLER_TERMINAL_COMMISSION_CHANGE
```

### Envers (partial — field level)

`SellerTerminalJpaEntity` uses field-level `@Audited(withModifiedFlag = true)` on control and financial fields only. Class-level `@Audited` is forbidden — it would audit PII and high-churn fields.

Audited fields:

```text
terminal_code
status
commission_rate
odds_profile_id
limit_profile_id
blocked_at / blocked_by / blocked_reason
disabled_at
```

Excluded (no `@Audited`):

```text
first_name, last_name, phone_number, address_*  — PII
last_seen_at                                    — high-churn
external_provider / external_issuer / external_subject
created_at, updated_at
```

Revision table: `revinfo`, owned by the `platform.entityhistory` revision entity/listener.

`ticket` has no Envers — commission snapshot columns are immutable once written.

## Migration Strategy

Target migration: `V100`.

Includes:
- `seller_terminal` table.
- `seller_terminal_aud` Envers table.
- `revinfo` revision table (if not already present).
- `ticket` column additions (`seller_terminal_id`, `seller_commission_rate_snapshot`, `seller_commission_amount_snapshot`).

Rollout:
- New sales use `seller_terminal_id`.
- Legacy cashier flows remain temporarily.
- Admin UI wording moves to "Vendeurs / Terminaux".
- Remove legacy cashier after production parity.
