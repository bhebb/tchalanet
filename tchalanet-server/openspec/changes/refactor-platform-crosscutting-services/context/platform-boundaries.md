# Context — Platform Cross-cutting Boundaries

## Current Rule

`common` is a technical shared kernel. It can contain primitives and infrastructure helpers:

- bus contracts;
- context primitives;
- typed IDs;
- web primitives;
- cache abstractions;
- transaction helpers;
- thin persistence base classes/listeners;
- RLS bridge/session variables.

`platform` owns stateful transversal application capabilities with tables, lifecycle, tenant/user
policy, and operational workflows.

## Boundary Matrix

| Class/Concern | Target | Reason |
|---|---|---|
| `BaseEntity` | `common.persistence` | Technical audit fields only |
| `BaseTenantEntity` | `common.persistence` | Technical tenant column support only |
| JPA timestamps/listeners | `common.persistence` | Technical entity lifecycle |
| RLS datasource/session bridge | `common.persistence.rls` | Technical database binding |
| Functional audit log | `platform.audit` | Business/user action history |
| Envers revision entity/listener metadata | `platform.audit.internal.persistence` | Technical revision metadata enriched with runtime context |
| Role assignment | `platform.accesscontrol` | Stateful authorization policy |
| Permission grant/revoke | `platform.accesscontrol` | Stateful authorization policy |
| HTTP idempotency record | `platform.idempotence` | Client retry workflow |
| Processed event record | `platform.idempotence` | Event consumer replay workflow |

## Non-negotiables

- Functional audit does not belong in `common`.
- Access-control tables do not belong in `common`.
- Idempotency records and processed-event records do not belong in `common`.
- Access control must not import `core` or `features`.
- Audit must not replace domain events or state invariants.
- Idempotency must not replace domain locks, unique constraints or state transitions.

## Decision Heuristic

If a concern has a table, lifecycle, tenant/user application policy, or workflow, it probably belongs
in `platform`, not `common`.

If a concern validates resource state, action eligibility, or business invariants, it belongs in the
owning domain under `core`, not in a platform cross-cutting capability.

## Current Inventory Decision

`common.persistence.audit.TenantEntityListener` may remain in `common` because it is technical entity
metadata binding. It is not functional audit and it does not own audit tables, audit search, audit
policy, role assignment, permissions, idempotency records, or processed-event workflow.
