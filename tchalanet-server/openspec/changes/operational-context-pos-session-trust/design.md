# Design: operational-context-pos-session-trust

## 1. Mental model

```text
OperationalContextHint
  = what the HTTP request claims as the POS frame

ValidatedPosOperationContext
  = what core.session has verified as true for this action

OfflineGrant
  = temporary right to sell offline in that validated frame

OfflineSubmission
  = offline sale received later; not yet a ticket

Ticket
  = official sale accepted by core.sales
```

Normative sentence:

```text
POS context headers are operational claims, not business proof.
They may be accepted only when the owning domain validates the frame late and the action policy explicitly allows the claim trust level.
Offline grants and offline sync must never rely on weak client claims alone.
```

## 2. Request pipeline

```text
BearerTokenAuthenticationFilter
  -> UserBootstrapFilter
  -> TchContextFilter
       -> ApiScopeResolver
       -> TenantContextResolver
       -> ActorContextResolver
       -> TenantOverrideResolver
       -> OperationalContextResolver
       -> TchContextBinder.bind(finalCtx)
```

`OperationalContextResolver` does:

```text
1. Read `OperationalContextHeaders` only.
2. Parse terminalId/outletId/salesSessionId as typed IDs.
3. Read declared source, if present.
4. Verify technical proof if source requires proof.
5. Derive OperationalContextTrust server-side.
6. Attach Optional<OperationalContextHint> to TchRequestContext.
```

`OperationalContextResolver` does not:

```text
- query terminal/outlet/session tables
- validate tenant ownership
- check terminal locked/blocked status
- check outlet active status
- check session open/finalized status
- decide whether the action is allowed
```

## 3. Body vs headers

Headers carry execution context claims:

```http
X-Tch-Terminal-Id: <uuid>
X-Tch-Outlet-Id: <uuid>
X-Tch-Sales-Session-Id: <uuid>
X-Tch-Operational-Source: CLIENT_CLAIM|SIGNED_DEVICE_BINDING|ADMIN_SELECTION
```

Request body carries only business payload:

```json
{
  "lines": [],
  "amount": 100,
  "gameCode": "BORLETTE"
}
```

Forbidden in tenant/POS HTTP bodies:

```json
{
  "terminalId": "...",
  "outletId": "...",
  "salesSessionId": "..."
}
```

If POS-frame IDs appear in a request body for protected POS endpoints, return `400` with problem code/type:

```text
operational_context.in_body
https://tchalanet.dev/problems/operational-context-in-body
```

## 4. Common model

```java
public final class OperationalContextHeaders {
    public static final String TERMINAL_ID = "X-Tch-Terminal-Id";
    public static final String OUTLET_ID = "X-Tch-Outlet-Id";
    public static final String SALES_SESSION_ID = "X-Tch-Sales-Session-Id";
    public static final String OPERATIONAL_SOURCE = "X-Tch-Operational-Source";
    public static final String TENANT_OVERRIDE = "X-Tch-Tenant-Override";
    public static final String OVERRIDE_REASON = "X-Tch-Override-Reason";

    private OperationalContextHeaders() {}
}
```

```java
public record OperationalContextHint(
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    OperationalContextSource source,
    OperationalContextTrust trust
) {}
```

`TchRequestContext` exposes:

```java
Optional<OperationalContextHint> operationalContext();
OperationalContextHint operationalContextRequired();
```

No `validatedPosContext()` and no `trustedOperationalContextRequired()` in `TchRequestContext`.

## 5. Trust derivation

| Declared source | Server proof | Derived source | Derived trust | Behavior |
|---|---|---|---|---|
| none | none | NONE | NONE | no POS hint |
| CLIENT_CLAIM | none | CLIENT_CLAIM | WEAK | attach weak hint |
| SIGNED_DEVICE_BINDING | valid binding proof | SIGNED_DEVICE_BINDING | STRONG | attach strong hint |
| SIGNED_DEVICE_BINDING | missing/invalid proof | n/a | n/a | reject 401 |
| ADMIN_SELECTION | signed server selection token valid | ADMIN_SELECTION | STRONG | attach strong hint |
| ADMIN_SELECTION | no signed token in V1 | CLIENT_CLAIM | WEAK | downgrade + warn log |

Trust must never be accepted from a header.

## 6. Tenant override is separate

Tenant override headers:

```http
X-Tch-Tenant-Override: <tenant-id>
X-Tch-Override-Reason: support investigation for ticket ABC
```

The resolver may produce an effective tenant override only after:

```text
1. authenticated actor has SUPER_ADMIN authority
2. actor has permission platform.tenant.override
3. override tenant exists/is allowed
4. reason is non-blank
5. audit metadata is captured
```

This produces tenant/effective-context metadata. It does not change `OperationalContextSource`.

## 7. Authoritative POS resolution in core.session

Public API:

```text
core.session.api.query.ResolvePosOperationContextQuery
core.session.api.query.PosOperationAction
core.session.api.model.ValidatedPosOperationContext
```

Query:

```java
public record ResolvePosOperationContextQuery(
    TenantId tenantId,
    UserId actorUserId,
    OperationalContextHint hint,
    PosOperationAction action
) implements Query<ValidatedPosOperationContext> {}
```

Action enum:

```java
public enum PosOperationAction {
    ADMIN_POS_SELL,
    SELL_TICKET_ONLINE,
    REQUEST_OFFLINE_GRANT,
    SYNC_OFFLINE_SALES,
    PAYOUT,
    CLOSE_SESSION
}
```

Result:

```java
public record ValidatedPosOperationContext(
    TenantId tenantId,
    UserId actorUserId,
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    OperationalContextSource source,
    OperationalContextTrust trust,
    Instant resolvedAt
) {}
```

Handler validation order:

```text
1. hint exists
2. source/trust acceptable for action via PosActionPolicy
3. tenant coherent
4. terminal exists
5. terminal belongs to tenant
6. terminal not blocked/locked
7. outlet exists
8. outlet belongs to tenant
9. outlet active
10. session exists
11. session belongs to tenant
12. session matches terminal/outlet/user or permitted admin POS mode
13. session is open
14. action is permitted
```

Usage:

```java
var hint = ctx.operationalContextRequired();
var pos = queryBus.ask(new ResolvePosOperationContextQuery(
    ctx.effectiveTenantIdRequired(),
    ctx.userIdRequired(),
    hint,
    PosOperationAction.REQUEST_OFFLINE_GRANT
));
```

## 8. Action policy

`PosActionPolicy` is the single place that maps actions to accepted trust.

```java
public final class PosActionPolicy {
  public void requireTrustFor(PosOperationAction action, OperationalContextTrust trust) {
    // no silent default
  }
}
```

V1 table:

| Action | Minimum trust V1 | Notes |
|---|---|---|
| ADMIN_POS_SELL | WEAK | Accepted because core.session validates the full frame. |
| SELL_TICKET_ONLINE | WEAK or STRONG by feature flag | Target is STRONG. |
| REQUEST_OFFLINE_GRANT | STRONG | Device proof required. |
| SYNC_OFFLINE_SALES | STRONG | Grant + device proof + signature. |
| PAYOUT | STRONG recommended | Can temporarily be WEAK only with explicit policy decision. |
| CLOSE_SESSION | STRONG recommended | Depends on seller/admin mode. |

## 9. Offline grant

`RequestOfflineGrantCommandHandler` must:

```text
1. read tenant/user/hint from command or current context
2. call ResolvePosOperationContextQuery with REQUEST_OFFLINE_GRANT
3. require STRONG trust
4. validate offline config/quota/device/session
5. issue and persist OfflineGrant
6. return grant token/config to POS/mobile
```

Weak claims alone must fail:

```text
CLIENT_CLAIM/WEAK + no device proof -> offlinesync.device_proof_required
```

Grant limits:

```text
validFrom
validUntil
maxTickets
maxTotalAmount
allowedGames / channels if needed
deviceId
sellerUserId
terminalId
outletId
salesSessionId
```

## 10. Offline sync

`offline_grant != ticket` and `offline_submission != ticket`.

```text
offline_submission received
  -> technical validation in core.offlinesync
  -> business validation in sales/draw/limit/cutoff
  -> accepted: create real ticket through core.sales
  -> rejected: rejected/review
```

## 11. Audit

Sensitive actions must audit the **validated frame**, not raw hint headers.

Minimum metadata:

```text
tenantId
actorUserId
effectiveTenantId
overrideTenantId if present
overrideReason if present
terminalId
outletId
salesSessionId
operationalSource
operationalTrust
action
result
grantId / ticketId / submissionId when applicable
```

## 12. Open questions moved to follow-up

1. Signed device binding format.
2. Signed admin selection token format.
3. Sunset date for `SELL_TICKET_ONLINE` accepting WEAK.
4. Whether admin POS should attach to an existing seller session or create admin-flagged session.
