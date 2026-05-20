# Design — operational-context-offline-sync

## 1. Request pipeline (accepted)

```text
1. IDENTIFY   resolve tenant, user, roles, API scope
2. ATTACH     attach operational context candidate if available
3. AUTHORIZE  apply method security / permission evaluator
4. VALIDATE   validate operational invariants in the domain use case before mutation
```

`TchContextFilter` is the **single** canonical producer:

```text
TchContextFilter
  -> contextFactory
  -> tenantContextResolver
  -> actorContextResolver
  -> operationalContextResolver         (new)
  -> contextBinder.bind(finalCtx)
```

### Rejected alternative

```text
Keep OperationalContextFilter as a second canonical producer
```

Reason: a second filter rebinding context creates ordering and ownership ambiguity, and there is no race-free guarantee that downstream filters see the same context the controller sees.

## 2. Trust model

```java
public enum TrustLevel { NONE, WEAK, STRONG }

public enum OperationalContextSource {
    NONE(TrustLevel.NONE),
    CLIENT_CLAIM(TrustLevel.WEAK),
    SERVER_BOOTSTRAP(TrustLevel.STRONG),
    SIGNED_DEVICE_BINDING(TrustLevel.STRONG),
    ADMIN_SELECTION(TrustLevel.STRONG);

    public boolean isTrustedForSensitiveOperation() {
        return trustLevel == TrustLevel.STRONG;
    }
}
```

`OperationalRequestContext` carries only `(terminalId, outletId, salesSessionId, source)`. The boolean `selectedByAdmin` is removed.

`TchRequestContext.trustedOperationalContextRequired()` throws if `operationalContext == null`, source is not trusted, `terminalId`/`outletId` missing, or `salesSessionId` missing when the use case requires it. It does **not** validate terminal-locked / outlet-blocked / session-open — those checks belong to domain validators.

## 3. Operational context resolution

Owner: `core.terminal`. Query:

```text
GetCurrentOperationalContextQuery(tenantId, userId, terminalIdHeader, deviceId, terminalBinding)
  -> CurrentOperationalContextView(terminalId, outletId, salesSessionId, source)
```

Role rules:

```text
CASHIER / OPERATOR            attempt automatic resolution
TENANT_ADMIN / SUPER_ADMIN    require explicit admin POS selection or trusted device binding
SYSTEM                        no POS operational context by default
```

## 4. Admin POS selection

Endpoints:

```http
POST   /tenant/me/operational-context/select
GET    /tenant/me/operational-context
DELETE /tenant/me/operational-context
```

Selection requirements: permission-checked, tenant-bound, terminal-bound, audited, short-lived, revocable. Source = `ADMIN_SELECTION`.

## 5. Validation model

Atomic validators (via `QueryBus`) — already present:

- `core.terminal.ValidateTerminalForOperationQuery`
- `core.outlet.ValidateOutletForOperationQuery`
- `core.session.ValidateSalesSessionForOperationQuery`

Use-case validators (composers):

- `core.sales.PosSaleOperationValidator` (present)
- `core.sales.PosCancelOperationValidator` (new)
- `core.sales.OfflineSaleAcceptanceValidator` (new — enforces §15/§16/§17)
- `core.payout.PosPayoutOperationValidator` (new)
- `core.offlinesync.IssueOfflineGrantOperationValidator` (present)
- `core.offlinesync.ReceiveOfflineBatchOperationValidator` (present)

## 6. Fail-fast order

```text
1. trusted operational context
2. terminal exists / tenant
3. terminal locked / blocked / seller assignment
4. outlet exists / tenant
5. outlet status / blocked flags
6. session exists / tenant
7. session terminal/outlet/seller match
8. session status
9. action-specific business gates
```

## 7. Concurrency

Between validation and commit:

```text
terminal can be locked
outlet can close day
session can be closed/finalized
```

Critical handlers (`SellPosTicket`, `RequestPayout`, `IssueOfflineGrant`, `ReceiveOfflineBatch`, `SyncOfflineSales`, `AdminApproveOfflineSubmission`) must apply at least one of:

- optimistic locking / version guard
- minimal re-check inside transaction before mutation
- DB constraint / partial unique index

V1 decision: no cache; validate snapshot; re-check minimal critical state inside the transaction before save.

## 8. Cache policy

```text
v1: no cache for operational validation
v2: short TTL cache with event-driven eviction
    L1 <= 5s, L2 <= 15s, max <= 30s
    evict on terminal lock/unlock, outlet block/day close, session close/finalize
```

## 9. Offline sync invariants

```text
OfflineSaleSubmission != Ticket
OfflineCodeReservation != Ticket
Offline receipt != server-accepted ticket
```

- `core.offlinesync` owns: grants, code batches, code reservations, offline batches, submissions, payload hash, signature, technical rejects, sales-decision projection.
- `core.sales` owns: ticket, ticket_line, official sales events, sales acceptance/rejection decisions.

### FINALIZED session

```text
status = SALES_REVIEW_REQUIRED
salesRejectReason = SESSION_FINALIZED
riskFlags += FINALIZED_SESSION
```

No separate quarantine table in v1.

### Draw result known

```text
no auto-accept
review or reject according to tenant policy
risk flag RESULT_KNOWN_AT_SYNC
```

### Device time

Never sufficient proof; combine with grant issue time, validity window, server `receivedAt`, draw cutoff, result-known timestamp, clock drift flags.

## 10. Events

Offline-sync events (technical):

```text
OfflineGrantIssuedEvent
OfflineBatchReceivedEvent
OfflineSubmissionTechnicallyRejectedEvent
OfflineBatchReadyForSalesEvent
OfflineSubmissionSalesDecisionRecordedEvent
```

Sales events (official):

```text
TicketPlacedEvent
TicketCancelledEvent
TicketResultedEvent
TicketPayoutMarkedPaidEvent
```

Only `TicketPlacedEvent` is consumed by official stats / session / ledger / payout flows.

## 11. Anti-patterns

```text
trust terminalId/outletId/sessionId only because they are in context
keep OperationalContextFilter as a second canonical producer
keep selectedByAdmin boolean
trust CLIENT_CLAIM for sensitive operations
create sales.ticket inside offlinesync
count offline submissions as official sales
auto-accept after draw result is known
auto-accept FINALIZED session submissions
cache operational validation in v1
let Sales read terminal/outlet/session repositories directly
let Offlinesync parse Sales internals
```
