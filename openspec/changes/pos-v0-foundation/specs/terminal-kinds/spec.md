# Spec 01 — Terminal entity (kinds, ownership, lifecycle)

## Domain

`core.terminal` (renamed from `core.pos` in change `rename-pos-to-terminal`).

## ADDED Requirements

### Requirement: Terminal has an immutable `kind` field (PHYSICAL or VIRTUAL)

The `Terminal` aggregate SHALL carry a `kind` field of type `TerminalKind` (PHYSICAL | VIRTUAL).
`kind` is set at creation and SHALL NOT change thereafter.
A PHYSICAL terminal SHALL NOT have an `ownerAgentId`.
A VIRTUAL terminal SHALL have an `ownerAgentId`.

#### Scenario: VIRTUAL terminal requires ownerAgentId

- **WHEN** `Terminal.createVirtual(...)` is called without an `ownerAgentId`
- **THEN** an `IllegalArgumentException` is thrown

#### Scenario: PHYSICAL terminal must not have ownerAgentId

- **WHEN** `Terminal.createPhysical(...)` is called with an `ownerAgentId`
- **THEN** an `IllegalArgumentException` is thrown

#### Scenario: kind is immutable

- **WHEN** any command is applied to a `Terminal` after creation
- **THEN** the `kind` field retains its original value

---

### Requirement: Terminal has a mutable `status` (ACTIVE | DISABLED | ARCHIVED)

The `Terminal` aggregate SHALL carry a `status` field. Allowed transitions:

- ACTIVE → DISABLED
- DISABLED → ACTIVE
- ACTIVE | DISABLED → ARCHIVED

ARCHIVED is terminal — no further transition.

#### Scenario: Cannot transition from ARCHIVED

- **WHEN** `disable()`, `enable()`, or `rename()` is called on an ARCHIVED terminal
- **THEN** a domain exception is thrown with code `TERMINAL_ARCHIVED`

#### Scenario: Status persisted

- **WHEN** `ChangeTerminalStatusCommand` is handled
- **THEN** the status column in `terminal` reflects the new value

---

### Requirement: Single ACTIVE virtual terminal per agent

At most one VIRTUAL terminal with status ACTIVE SHALL exist per `(tenantId, ownerAgentId)`.
This is enforced by a partial unique index.

#### Scenario: Duplicate ACTIVE virtual terminal rejected

- **WHEN** a second `ProvisionVirtualTerminalCommand` is received for the same agent who already has an ACTIVE VIRTUAL terminal
- **THEN** the command is idempotent and returns the existing terminal (no new row inserted)

---

### Requirement: `ProvisionVirtualTerminalCommand` is idempotent

If an ACTIVE VIRTUAL terminal already exists for the given agent, the command SHALL return it without inserting a new row.
If none exists, it creates one.

#### Scenario: Returns existing terminal

- **WHEN** `ProvisionVirtualTerminalCommand(agentId, outletId)` is executed and an ACTIVE VIRTUAL terminal already exists for that agent
- **THEN** the existing terminal is returned and no INSERT is performed

#### Scenario: Creates new terminal

- **WHEN** no ACTIVE VIRTUAL terminal exists for that agent
- **THEN** a new VIRTUAL terminal is inserted and returned

---

### Requirement: Owner agent of VIRTUAL terminal may update `displayName` only

When the caller is the owner agent (not TENANT_ADMIN), `UpdateTerminalCommand` SHALL only allow updating `displayName`.
Any attempt to change `outletId` or other fields SHALL be rejected with 403.

#### Scenario: Owner agent updates display name

- **WHEN** `UpdateTerminalCommand` is handled and caller has `terminal.self.write`, `kind=VIRTUAL`, `ownerAgentId=ctx.appUserId`
- **THEN** the `displayName` is updated

#### Scenario: Owner agent cannot reassign outlet

- **WHEN** `UpdateTerminalCommand` carries an `outletId` and caller is the owner agent (not TENANT_ADMIN)
- **THEN** 403 is returned

---

### Requirement: Terminal Ports

The following ports SHALL be implemented:

```java
Optional<Terminal> findById(TerminalId id);
Optional<Terminal> findActiveVirtualForAgent(AppUserId agentId);
List<Terminal> listByOutlet(OutletId outletId, TerminalStatus status);
Terminal save(Terminal terminal);
```

#### Scenario: findActiveVirtualForAgent returns empty when none exists

- **WHEN** no ACTIVE VIRTUAL terminal exists for the given agent
- **THEN** `findActiveVirtualForAgent` returns `Optional.empty()`

---

### Requirement: Terminal events emitted after-commit

The following events SHALL be published after commit:

- `TerminalCreatedEvent` on any terminal creation
- `TerminalStatusChangedEvent` on status transition
- `TerminalReassignedEvent` on outlet reassignment

#### Scenario: TerminalStatusChangedEvent carries old and new status

- **WHEN** `ChangeTerminalStatusCommand` transitions from ACTIVE to DISABLED
- **THEN** `TerminalStatusChangedEvent(oldStatus=ACTIVE, newStatus=DISABLED)` is published

---

### Requirement: Terminal cache

Two named caches SHALL be maintained:

| Cache                             | TTL L1 | TTL L2 | Eviction events                                                                 |
| --------------------------------- | ------ | ------ | ------------------------------------------------------------------------------- |
| `core.terminal.by_id`             | 60 s   | 5 min  | `TerminalCreatedEvent`, `TerminalStatusChangedEvent`, `TerminalReassignedEvent` |
| `core.terminal.virtual_for_agent` | 60 s   | 5 min  | same                                                                            |

#### Scenario: Cache evicted on status change

- **WHEN** `TerminalStatusChangedEvent` is received by the cache eviction listener
- **THEN** both cache entries for the affected terminal are invalidated

---

### Requirement: Schema additions applied in-place to existing migration

The following DDL SHALL be added to the existing migration file that creates `terminal` (no new migration file created):

```sql
ALTER TABLE terminal
  ADD COLUMN IF NOT EXISTS kind            varchar(16) NOT NULL DEFAULT 'PHYSICAL',
  ADD COLUMN IF NOT EXISTS owner_agent_id  uuid        NULL,
  ADD COLUMN IF NOT EXISTS status          varchar(16) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE terminal ADD CONSTRAINT chk_terminal_kind   CHECK (kind   IN ('PHYSICAL','VIRTUAL'));
ALTER TABLE terminal ADD CONSTRAINT chk_terminal_status CHECK (status IN ('ACTIVE','DISABLED','ARCHIVED'));
ALTER TABLE terminal ADD CONSTRAINT chk_terminal_kind_owner CHECK (
  (kind = 'PHYSICAL' AND owner_agent_id IS NULL) OR
  (kind = 'VIRTUAL'  AND owner_agent_id IS NOT NULL)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_terminal_one_active_virtual_per_agent
  ON terminal (tenant_id, owner_agent_id)
  WHERE kind = 'VIRTUAL' AND status = 'ACTIVE' AND deleted_at IS NULL;
```

#### Scenario: Schema validates after migration

- **WHEN** a fresh DB is created and all migrations run
- **THEN** `ddl-auto=validate` passes for the `terminal` table
