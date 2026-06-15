# Change: SellerTerminal V0

## Status

Proposed

## Why

The V0 product pivot shows that field sellers are not managed as full tenant users, cashier users, outlets, or sales sessions.

The operator wants a simple model:

```text
one activated seller/terminal = one seller identity + one selling access + one control unit + one billing unit
```

Tenant admins create seller terminals, assign a terminal code/PIN/password, set a default commission, attach odds and limit profiles, and can block or disable the terminal immediately.

Seller terminals authenticate through Firebase technical users in V0, but their business authorization remains controlled by Tchalanet.

## What Changes

- Introduce `SellerTerminal` as the V0 operational seller model.
- Replace new field-seller flows based on `CASHIER`/seller user/outlet/session with a single SellerTerminal profile.
- Store seller identity fields directly on SellerTerminal:
  - first name;
  - last name;
  - display name;
  - phone;
  - address fields.
- Store selling/control fields directly on SellerTerminal:
  - terminal code;
  - external provider/subject;
  - status;
  - commission rate;
  - odds profile;
  - limit profile;
  - optional outlet.
- Add immediate block/unblock/disable/reset access actions.
- Add commission snapshot to sales.
- Keep outlet optional.
- Keep sales session out of V0.
- Make SellerTerminal the selling actor for Flutter POS.

## Domain Placement

SellerTerminal belongs in:

```text
core.terminal
```

Rationale:

- It is a business-critical operational actor.
- Its status directly controls whether sales are allowed.
- Blocking a terminal has immediate financial/control impact.
- Commission, odds profile, and limit profile affect settlement/reporting.
- It must be validated by core sales before ticket creation.

It should not live in:

```text
platform.identity
```

because it is not a generic identity concern.

It should not live in:

```text
features
```

because features must not own business invariants.

It should not live only in `catalog`

because terminal state and commission are mutable business control data, not read-mostly reference data.

## Scope

Backend:

- `core.terminal`
- `core.sales`
- `core.pricing` or existing odds/pricing domain
- `core.limitpolicy`
- `features.tenantadmin`
- `features.bootstrap`
- `platform.audit`
- `platform.identity` integration for Firebase technical users only

Database:

- `seller_terminal`
- `seller_terminal_external_identity` if not created by access-context change
- `ticket` sale snapshots for seller terminal and commission

Frontend contracts:

- Admin web terminal management endpoints
- Terminal bootstrap/me endpoint for Flutter
- Summary rows for dashboard/stats

## Non-Goals

- Offline sales.
- Sales sessions.
- Mandatory outlets.
- Full seller AppUser / TenantMembership flow.
- Full commission settlement workflow.
- Full billing implementation.
- Flutter UI implementation.
- Dashboard UI implementation.
- Replacing existing legacy cashier data in one migration.

## Impact

- New V0 sales should use `seller_terminal_id`.
- Existing `CASHIER` role becomes legacy compatibility only.
- Admin screens should expose "Vendeurs / Terminaux", not "Cashiers".
- Reporting should aggregate by SellerTerminal.
- Ticket details remain internal/detail views; admins primarily see summaries.

## Rollout

1. Add SellerTerminal schema and domain model.
2. Add admin create/update/block/unblock/disable/reset access commands.
3. Provision Firebase technical user when creating a SellerTerminal.
4. Resolve Firebase UID to SellerTerminal in the auth pipeline.
5. Add SellerTerminal validation to sales.
6. Add commission snapshot on tickets.
7. Migrate admin UI wording and dashboard summaries.
8. Deprecate legacy cashier flow after parity.
