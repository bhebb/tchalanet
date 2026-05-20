# Operational Context Architecture

## Status

Normative target for the post-platform refactor.

## Purpose

Operational context answers a different question from identity and request context.

| Concept | Question answered | Home |
|---|---|---|
| Request context | Who/what is calling right now? Which tenant/scope/request? | `common.context` + `common.context.web` |
| Identity | Who is this app user persistently? Which profile/memberships? | `platform.identity` |
| Access control | What is this actor allowed to do? | `platform.accesscontrol` |
| Operational context | Is this actor allowed to operate in this terminal/outlet/session frame for this use case? | Resolver/use-case layer; initially `platform.operationalcontext` only if reused broadly |

## Decision

Do not put operational context into `platform.identity`.

Operational context is runtime/application state composed from:

- `common.context.TchRequestContext`
- `platform.identity.api.IdentityApi`
- `platform.accesscontrol.api.AccessControlApi`
- `core.outlet.api`
- `core.terminal.api`
- `core.session.api`
- use-case-specific input headers/body fields

## Glossary

Request context:
Runtime execution facts for the current call or job, including tenant, actor, scope, request id, and bound operational request metadata. Owned by `common.context`.

Identity:
Persistent user facts such as app user, profile, preferences, tenant membership, and IdP mapping. Owned by `platform.identity`.

Access control:
Permission and role decisions for an actor within a tenant or platform scope. Owned by `platform.accesscontrol`.

Operational context:
Resolved use-case frame that proves an actor can operate against a terminal/outlet/session combination before a business action runs.

## Initial placement

For POS/seller flows, use a dedicated resolver package:

```text
platform.operationalcontext.api
platform.operationalcontext.internal.service
```

Only create `platform.operationalcontext` if at least two core/features need the same seller/admin resolution logic. Otherwise keep a resolver inside the owning use case/module and promote it later.

## Seller operational context

Typical input:

```text
tenantId from TchRequestContext
actorUserId from TchRequestContext
terminalId from header/body
outletId from header/body or terminal lookup
salesSessionId from header/body
```

Typical output:

```text
SellerOperationalContext
  tenantId
  actorUserId
  terminalId
  outletId
  sessionId
  permissions/effective role snapshot if needed
  locale/timezone/currency effective values if needed
```

## RLS rule

Operational context does not bypass RLS.

- Tenant-scoped system/user work binds the tenant in `TchRequestContext`.
- Super-admin override remains explicit and auditable.
- `SYSTEM` execution scope does not imply cross-tenant access.

## Validation rule

A use case must validate its operational frame before mutating critical state.

Example for sell:

1. Resolve request context.
2. Resolve seller operational context.
3. Verify terminal belongs to outlet/tenant and is active.
4. Verify session is open and belongs to actor/terminal/outlet when required.
5. Verify permission via `platform.accesscontrol`.
6. Execute business command in `core.sales`.
