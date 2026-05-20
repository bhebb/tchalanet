# Convention — User Context & Operational Context

## Final rule

```text
Global context is validated early.
Operational context is attached early.
Operational context is validated late, per action.
```

## Pipeline

```text
TchContextFilter
  -> ApiScopeResolver
  -> TchRequestContextFactory
  -> TenantContextResolver
  -> ActorContextResolver
  -> OperationalContextHeaderParser
  -> TchContextBinder.bind(finalCtx)
```

No separate `OperationalContextFilter`.

## Package boundary

`common.context.operational` is runtime-only and neutral. It may define roles, sources, trust levels,
headers, hints, typed context records and missing/untrusted context exceptions.

It must not call repositories, `CommandBus`, `QueryBus`, platform APIs, core APIs, catalog APIs or
features. It must not validate terminal, outlet, sales session, seller assignment, payout
eligibility or offline sync eligibility.

## OperationalRequestContext bridge

```java
public record OperationalRequestContext(
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    OperationalContextSource source
) {}
```

The flat record is a compatibility bridge. New sensitive code should prefer typed helpers on
`TchRequestContext` and the role-aware types in `common.context.operational`.

The POS frame keeps `sellerUserId` separate from `actorUserId`. For online POS they often match.
For offline replay, the actor can be `SYSTEM` while `sellerUserId` remains the original cashier.

## Trusted sources

```text
SERVER_BOOTSTRAP
SIGNED_DEVICE_BINDING
ADMIN_SELECTION
SUPER_ADMIN_OVERRIDE
```

Untrusted for sensitive operation:

```text
CLIENT_CLAIM
NONE
```

## Required helper

Use:

```java
ctx.trustedOperationalContextRequired()
ctx.trustedPosOperationalContextRequired()
ctx.sellerOperationalContextRequired()
ctx.adminOperationalContextRequired()
ctx.superAdminOverrideRequired()
```

for:

```text
sell
payout
offline grant
offline sync
```

`CLIENT_CLAIM` is weak. It can be accepted only by use cases that explicitly opt into weak context.

## Owner boundaries

```text
core.terminal -> current operational context and terminal validation
core.outlet   -> outlet validation
core.session  -> session validation
core.sales    -> sell/cancel/offline acceptance validators
core.payout   -> payout validator
core.offlinesync -> grant/sync technical validators
```

## Fail-fast order

```text
1. trusted operational context
2. terminal exists / tenant
3. terminal locked / blocked / seller assignment
4. outlet exists / tenant
5. outlet status / blocked flags
6. session exists / tenant
7. session terminal/outlet/seller match
8. session status
9. action-specific gates
```

## Concurrency

Critical handlers must not rely only on a stale snapshot. Use:

```text
version guard
transactional re-check
DB constraint
```

## Admin POS mode

Admins must explicitly select POS/operator mode.

```http
POST   /tenant/me/operational-context/select
GET    /tenant/me/operational-context
DELETE /tenant/me/operational-context
```

Source:

```text
ADMIN_SELECTION
```

An admin does not automatically become a seller. Target handlers still validate permission,
terminal, outlet, session and action-specific invariants.

## Role matrix

| Operation | Seller | Admin | Super-admin | System |
| --- | --- | --- | --- | --- |
| Sell ticket | POS context required | Explicit admin POS selection required | Tenant override + explicit POS context required | Not allowed |
| Payout | POS context required | Explicit admin POS selection + permission required | Override + POS context + permission required | Not allowed |
| Offline grant | Domain policy required | Permission and explicit policy required | Override + permission required | Controlled system flow only |
| Offline sync replay | Original seller in payload | Not the replay actor | Not the replay actor | Allowed as actor, preserves original seller |
| Tenant admin management | Not allowed | Permission required | Override + permission required | Not allowed |
