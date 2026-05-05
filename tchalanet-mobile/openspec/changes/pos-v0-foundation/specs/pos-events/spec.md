# Spec 06 — Domain events emitted by the foundation

## Domain

Cross-cutting — events published by `core.terminal`, `core.outlet`, `core.session`, `catalog.settings`.

## ADDED Requirements

### Requirement: All foundation events carry a UUID `eventId`

Every event record listed in this spec SHALL include a `UUID eventId` field populated at publication time (not at construction).
Listeners use `eventId` to implement idempotency via `ProcessedEventPort`.

#### Scenario: eventId populated

- **WHEN** any foundation event is published
- **THEN** `event.eventId()` returns a non-null UUID

---

### Requirement: All events emitted after-commit

Every event listed in this spec SHALL be published via Spring's `@TransactionalEventListener(phase = AFTER_COMMIT)`.
No event is published if the originating transaction rolls back.

#### Scenario: No event on rollback

- **WHEN** a command handler throws a domain exception before commit
- **THEN** no event is published

---

### Requirement: Terminal events catalogue published

The following records SHALL be published by `core.terminal`:

```java
TerminalCreatedEvent(TerminalId id, TenantId tenantId, OutletId outletId, TerminalKind kind, AppUserId ownerAgentId, UUID eventId)
TerminalStatusChangedEvent(TerminalId id, TenantId tenantId, TerminalStatus oldStatus, TerminalStatus newStatus, UUID eventId)
TerminalReassignedEvent(TerminalId id, TenantId tenantId, OutletId oldOutletId, OutletId newOutletId, UUID eventId)
```

#### Scenario: TerminalStatusChangedEvent published on disable

- **WHEN** `ChangeTerminalStatusCommand(DISABLED)` is handled
- **THEN** `TerminalStatusChangedEvent(oldStatus=ACTIVE, newStatus=DISABLED)` is published after-commit

---

### Requirement: Outlet events catalogue published

The following records SHALL be published by `core.outlet`:

```java
OutletCreatedEvent(OutletId id, TenantId tenantId, OutletKind kind, AppUserId ownerAgentId, UUID eventId)
OutletUpdatedEvent(OutletId id, TenantId tenantId, Set<OutletField> changedFields, boolean juridicalTouched, UUID eventId)
OutletKindOrOwnerChangedEvent(OutletId id, TenantId tenantId, OutletKind oldKind, OutletKind newKind, AppUserId oldOwnerId, AppUserId newOwnerId, UUID eventId)
OutletStatusChangedEvent(OutletId id, TenantId tenantId, OutletStatus oldStatus, OutletStatus newStatus, UUID eventId)
```

#### Scenario: OutletKindOrOwnerChangedEvent carries both old and new owner

- **WHEN** `ReassignOutletOwnerCommand` is handled
- **THEN** `OutletKindOrOwnerChangedEvent.oldOwnerId` and `.newOwnerId` are both set

---

### Requirement: Session events catalogue published

The following records SHALL be published by `core.session`:

```java
SalesSessionOpenedEvent(SessionId id, TenantId tenantId, OutletId outletId, TerminalId terminalId,
    AppUserId appUserId, BigDecimal openingFloat, Instant openedAt, boolean viaAdminOverride, UUID eventId)
SalesSessionClosedEvent(SessionId id, TenantId tenantId, OutletId outletId, TerminalId terminalId,
    AppUserId appUserId, BigDecimal closingAmount, BigDecimal expectedAmount,
    BigDecimal variance, Instant closedAt, AppUserId closedBy, UUID eventId)
SalesSessionAbortedEvent(SessionId id, TenantId tenantId, OutletId outletId, TerminalId terminalId,
    AppUserId appUserId, String reason, Instant abortedAt, AppUserId abortedBy, UUID eventId)
```

#### Scenario: SalesSessionOpenedEvent marks admin override

- **WHEN** a TENANT_ADMIN opens a session on behalf of a user not assigned to the outlet
- **THEN** `SalesSessionOpenedEvent.viaAdminOverride=true`

---

### Requirement: Settings event catalogue published

The following record SHALL be published by `catalog.settings`:

```java
SettingChangedEvent(SettingKey key, SettingLevel level, ScopeIds scope,
    @Nullable String oldValue, String newValue, ChangeKind change, UUID eventId)
```

#### Scenario: SettingChangedEvent on hard-delete

- **WHEN** `HardDeleteSettingCommand` is handled
- **THEN** `SettingChangedEvent(change=HARD_DELETE)` is published

---

### Requirement: All 7 listeners implemented and idempotent

The following listeners SHALL be implemented, each with a unique `handler_key` for `ProcessedEventPort`:

| Listener                                        | Listens to                                         | Action                                                            | Handler key                             |
| ----------------------------------------------- | -------------------------------------------------- | ----------------------------------------------------------------- | --------------------------------------- |
| `TerminalCacheEvictListener`                    | Terminal\* events                                  | evict `core.terminal.by_id` and `core.terminal.virtual_for_agent` | `terminal.cache-evict`                  |
| `OutletCacheEvictListener`                      | Outlet\* events                                    | evict outlet caches                                               | `outlet.cache-evict`                    |
| `SettingsCacheEvictListener`                    | `SettingChangedEvent`                              | evict scoped setting cache entries                                | `setting.cache-evict`                   |
| `SalesSessionAbortOnTerminalDisabledListener`   | `TerminalStatusChangedEvent` (newStatus != ACTIVE) | dispatch `AbortSalesSessionsForTerminalCommand`                   | `session.abort-on-terminal-disabled`    |
| `SalesSessionAbortOnUserSuspendedListener`      | `UserSuspendedEvent` (from `core.user`)            | dispatch `AbortSalesSessionsForUserCommand`                       | `session.abort-on-user-suspended`       |
| `SalesSessionAbortOnOutletOwnerChangedListener` | `OutletKindOrOwnerChangedEvent`                    | abort previous owner's open sessions                              | `session.abort-on-outlet-owner-changed` |
| `SalesSessionTicketCountersListener`            | `TicketPlacedEvent` (from `core.sales`)            | increment session counters                                        | `sales-session.ticket-counters`         |

#### Scenario: Listener is idempotent on duplicate event

- **WHEN** any listener receives the same `eventId` twice
- **THEN** the action is executed only once; second invocation returns immediately

#### Scenario: SalesSessionAbortOnTerminalDisabledListener ignores ACTIVE transitions

- **WHEN** `TerminalStatusChangedEvent(newStatus=ACTIVE)` is received
- **THEN** no abort command is dispatched

---

### Requirement: Listener template uses `@TchTx` and `AFTER_COMMIT`

All listeners SHALL follow this structure:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@TchTx
public void on(SomeEvent event) {
  if (processedEvents.alreadyProcessed(HANDLER_KEY, event.eventId())) return;
  // perform work
  processedEvents.markProcessed(HANDLER_KEY, event.eventId());
}
```

#### Scenario: Listener runs in its own transaction

- **WHEN** a listener fails after `markProcessed`
- **THEN** the retry re-enters idempotency check and exits cleanly
