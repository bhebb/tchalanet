# Change: Provider-Neutral Access Context V1

## Status

Proposed

## Why

External authentication providers prove identity, but they must not become the source of truth for Tchalanet tenant membership, roles, permissions, runtime context, RLS, audit, seller-terminal authorization, odds, limits, or sales rights.

The backend has provider-neutral authentication and access-control foundations, but the runtime contract between external identity, Tchalanet actor resolution, effective tenant, `TchRequestContext`, Spring method security, and RLS must be defined as one deny-safe pipeline.

The V0 product pivot also introduces a simpler seller model:

```text
Admin user        = Firebase user -> AppUser -> tenant/platform access
Seller terminal   = Firebase technical user -> SellerTerminal -> controlled sales
```

Seller terminals are not tenant admin users. They are operational selling actors.

## What Changes

- Define the canonical backend pipeline from technical bearer authentication to Tchalanet actor, effective tenant, roles, permissions, `TchRequestContext`, and RLS binding.
- Resolve external subjects through Tchalanet-owned mappings.
- Support two authenticated actor types in V1:
  - `APP_USER` for tenant/platform admins;
  - `SELLER_TERMINAL` for terminal-based sellers.
- Keep provider claims out of Tchalanet business authorization.
- Resolve admin access from Tchalanet-owned access-control data.
- Resolve terminal access from Tchalanet-owned `SellerTerminal` state.
- Enrich Spring `Authentication` with Tchalanet authorities:
  - `ROLE_*` for AppUser roles;
  - `PERM_*` for effective permissions;
  - `ACTOR_*` for actor type.
- Make `TchRequestContext` the canonical runtime source for actor, tenant, access, operational context, audit, handlers, and RLS.
- Define explicit and auditable `SUPER_ADMIN` tenant override behavior.
- Define equivalent explicit context creation for batch and scheduler execution.
- Migrate controller authorization toward role checks for admin surfaces and permission checks for sensitive actions.
- Prepare the runtime context so `SellerTerminal` sales can be added immediately after auth.

## Scope

Backend only:

- `tchalanet-common`: neutral runtime context contracts, actor type, request attributes, binding.
- `tchalanet-platform`: identity and access-control resolution for AppUsers.
- `tchalanet-core`: terminal actor resolution contract, terminal status validation hooks, controller method-security migration only in this change.
- `tchalanet-features`: bootstrap/private runtime response alignment only.
- `tchalanet-app`: security filter ordering and runtime assembly.
- PostgreSQL/Flyway changes required by the approved identity/access/terminal identity data model.

## Dependencies And Existing Changes

This change integrates, but does not replace or duplicate:

- `provider-neutral-auth-provisioning-v1`: provider verification and provisioning boundaries.
- `access-control-v1`: roles, permissions, assignments, overrides, and effective permission rules.
- `private-bootstrap-v1`: authenticated runtime bootstrap response.
- `runtime-state-and-public-bootstrap-v1`: public/runtime bootstrap contracts.
- `tchalanet-security-login-terminal`: terminal and operational-context security.
- Upcoming `seller-terminal-v0`: SellerTerminal business model, commission, odds/limits linkage, terminal sales.

Conflicting schema or permission names SHALL be reconciled with `access-control-v1` before a migration is created.

## Impact

- Provider-issued roles and permissions stop influencing business authorization.
- Protected requests fail before controllers when Tchalanet actor or effective access cannot be resolved.
- Spring method security receives authorities derived only from Tchalanet DB state.
- RLS binding occurs only after effective tenant resolution and canonical context creation.
- Identity/access lookup tables needed before context exists may require explicit, reviewed RLS exceptions.
- Seller terminal identity lookup is supported without making the full seller profile a non-RLS bootstrap table.
- Existing controllers will be migrated incrementally; core handlers retain all business invariant checks.

## Non-Goals

- Frontend UI implementation for web or Flutter authentication.
- Custom Tchalanet JWT generation for seller terminals.
- Multi-tenant membership for normal AppUsers in V1.
- Tenant custom roles or a permission-editor UI.
- External-provider admin provisioning beyond required Firebase Admin SDK hooks.
- Distributed permission caching or event-based cache invalidation.
- Replacing business invariant checks with Spring Security.
- Reimplementing public/private bootstrap payloads owned by existing changes.
- Full `SellerTerminal` sales implementation; this change prepares the auth/context foundation.

## V0 Product Alignment

Tchalanet V0 is aligned around controlled terminal sales:

```text
Tenant owner/admin
  -> creates SellerTerminal
  -> configures odds, limits, commission
  -> views stats and reports
  -> blocks/unblocks terminals

SellerTerminal
  -> authenticates using provider technical identity
  -> sells tickets only if ACTIVE
  -> cannot access admin features
```

Field sellers are no longer modeled as AppUsers with a `CASHIER` role for new V0 flows. Existing `CASHIER` surfaces may remain as legacy compatibility during migration, but `SellerTerminal` is the target model.

## Rollout

1. Reconcile data model and existing active changes.
2. Introduce actor-neutral context input and pipeline tests.
3. Add AppUser identity/access resolution and authority enrichment.
4. Add SellerTerminal identity resolution hooks.
5. Bind canonical context before tenant-scoped database access.
6. Migrate controllers by surface, preserving handler invariant tests.
7. Remove legacy trust in provider roles/tenant claims after parity is proven.
