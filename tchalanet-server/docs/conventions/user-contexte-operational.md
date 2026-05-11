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
  -> OperationalContextResolver
  -> TchContextBinder.bind(finalCtx)
```

No separate `OperationalContextFilter`.

## OperationalRequestContext

```java
public record OperationalRequestContext(
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    OperationalContextSource source
) {}
```

No `selectedByAdmin`.

## Trusted sources

```text
SERVER_BOOTSTRAP
SIGNED_DEVICE_BINDING
ADMIN_SELECTION
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
```

for:

```text
sell
payout
offline grant
offline sync
```

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
