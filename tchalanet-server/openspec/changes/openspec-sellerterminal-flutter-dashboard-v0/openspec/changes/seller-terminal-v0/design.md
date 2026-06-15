# Design: SellerTerminal V0

## Decision

`SellerTerminal` is the V0 model for field sellers.

It combines:

```text
seller profile
+ terminal access
+ commission
+ operational status
+ odds profile
+ limit profile
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
- address
- externalProvider
- externalSubject
- status
- commissionRate
- oddsProfileId nullable
- limitProfileId nullable
- outletId nullable
- lastSeenAt
- activatedAt
- blockedAt
- blockedBy
- blockedReason
- disabledAt
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

## Firebase Technical User

V0 avoids custom Tchalanet terminal JWTs.

When an admin creates a SellerTerminal:

1. Backend provisions a Firebase technical user.
2. Backend stores provider identity:
   - provider = `FIREBASE`
   - issuer = Firebase issuer
   - externalSubject = Firebase UID
3. SellerTerminal is linked to that external subject.
4. Flutter logs in using terminal code + PIN/password mapped to Firebase credentials.
5. Backend verifies Firebase token and resolves it to SellerTerminal.

The terminal is still authorized by Tchalanet, not Firebase claims.

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

Audit mandatory:

```text
SELLER_TERMINAL_CREATE
SELLER_TERMINAL_UPDATE
SELLER_TERMINAL_BLOCK
SELLER_TERMINAL_UNBLOCK
SELLER_TERMINAL_DISABLE
SELLER_TERMINAL_RESET_ACCESS
SELLER_TERMINAL_COMMISSION_CHANGE
```

## Migration Strategy

- Add new table and columns first.
- New sales use `seller_terminal_id`.
- Legacy cashier flows remain temporarily.
- Admin UI wording moves to "Vendeurs / Terminaux".
- Remove legacy cashier after production parity.
