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

Seller terminals authenticate through an external provider (Firebase). Tchalanet never issues terminal JWTs — authentication is always delegated to the provider, authorization is always Tchalanet-owned.

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
- `platform.identity` integration for external provider (Firebase) technical user provisioning

Database:

- `seller_terminal`
- `seller_terminal_external_identity` if not created by access-context change
- `ticket` sale snapshots for seller terminal and commission

Frontend contracts:

- Admin web terminal management endpoints
- Terminal bootstrap/me endpoint for Flutter
- Summary rows for dashboard/stats

## Architectural decision

`SellerTerminal` is the sole selling actor model going forward.

The concepts of `Terminal`, `SalesSession`, and legacy `Seller` are **removed** — not deprecated, not kept in parallel. They will be cleaned out of the domain once `SellerTerminal` reaches parity.

`Outlet` is kept as an optional geographic/organizational grouping. A `SellerTerminal` may reference an outlet but does not require one.

Consequences:
- No `Terminal`, `SalesSession`, or `Seller` domain models in new code.
- Envers audit tables for those concepts removed from V101.
- Legacy DB tables (`terminal`, `sales_session`, `seller`, `seller_outlet_assignment`) remain as data until explicitly migrated or archived.
- New sales reference `seller_terminal_id`, not `terminal_id` or `sales_session_id`.

## Non-Goals

- Offline sales.
- Mandatory outlets.
- Full seller AppUser / TenantMembership flow.
- Full commission settlement workflow.
- Full billing implementation.
- Flutter UI implementation.
- Dashboard UI implementation.
- Deleting legacy terminal/session DB tables in this change.

## Impact

- New sales use `seller_terminal_id`.
- `CASHIER` role becomes legacy compatibility only.
- Admin screens expose "Vendeurs / Terminaux", not "Cashiers".
- Reporting aggregates by SellerTerminal.
- `Terminal`, `SalesSession`, `Seller` domain concepts are no longer extended.

## Rollout

1. Add SellerTerminal schema and domain model.
2. Add admin create/update/block/unblock/disable/reset access commands.
3. Provision Firebase technical user when creating a SellerTerminal.
4. Resolve Firebase UID to SellerTerminal in the auth pipeline.
5. Add SellerTerminal validation to sales.
6. Add commission snapshot on tickets.
7. Migrate admin UI wording and dashboard summaries.
8. Deprecate legacy cashier flow after parity.
