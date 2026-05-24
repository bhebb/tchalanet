# Change: add-core-agent

## Summary

Introduce `core.agent` as the domain that models tenant-scoped commercial affiliate networks:

```text
Tenant
  -> Agent level 1 / regional affiliate
      -> Agent level 2 / local affiliate
          -> users / sellers / outlets / terminals
```

The domain separates **User** from **Agent**:

```text
User  = identity/login/permissions actor
Agent = commercial responsibility / delegated selling scope
```

`core.agent` owns commercial zones, agent hierarchy, agent mandates, and user-agent assignments. It provides stable queries so `core.sales`, `core.limitpolicy`, `core.promotion`, `core.outlet`, `core.terminal`, `core.session`, reporting, and admin features can resolve the operational commercial scope without importing internals.

## Why

Market reality shows tenants operate through affiliates, supervisors, regional agents, local agents, POS, and sellers. Existing `User`, `Outlet`, and `Terminal` are not enough to represent:

- agent hierarchy;
- commercial territory / zone;
- parent/child affiliate control;
- limits by agent or zone;
- promotion eligibility by zone or agent;
- reports by affiliate network;
- future prepaid wallet and commissions.

Without `Agent`, the platform would overload `User`, `Outlet`, or `Terminal`, making sales, limits, promotions, commissions, and reporting ambiguous.

## Scope

- Add `core.agent` API and internals.
- Add tenant-scoped commercial zones seeded during tenant onboarding.
- Add agent hierarchy with V1 depth limit.
- Add agent mandates / allowed zones.
- Add user-agent assignments.
- Expose stable query API to resolve agent operational scope.
- Integrate outlet/terminal/session/sales context with `agentId` and `zoneId`.
- Prepare, but do not fully implement, prepaid wallet/commission engines.

## Non-goals

- No unlimited agent tree in V1.
- No full wallet/prepaid ledger in this change.
- No full commission engine in this change.
- No GIS map/polygon/geolocation enforcement in V1.
- No agent as mini-tenant. Agents remain tenant-scoped delegated actors.

## Core decision

```text
Tenant owns the platform scope.
Agent owns a delegated commercial scope.
User performs actions.
Outlet is the point of sale operated by an agent.
Terminal sells within an outlet.
```
