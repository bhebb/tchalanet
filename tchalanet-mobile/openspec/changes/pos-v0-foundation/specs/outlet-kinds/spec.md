# Spec 02 — Outlet kinds & operational fields

## Domain

`core.outlet` (extension).

## ADDED Requirements

### Requirement: Outlet has a `kind` field (FIXED | MOBILE | VIRTUAL)

The `Outlet` aggregate SHALL carry a `kind` field of type `OutletKind`.
FIXED outlets SHALL NOT have an `ownerAgentId`.
MOBILE and VIRTUAL outlets SHALL have an `ownerAgentId`.

#### Scenario: MOBILE outlet requires ownerAgentId

- **WHEN** an outlet of kind MOBILE is created without `ownerAgentId`
- **THEN** a domain exception is thrown

#### Scenario: FIXED outlet must not have ownerAgentId

- **WHEN** an outlet of kind FIXED is created with an `ownerAgentId`
- **THEN** a domain exception is thrown

---

### Requirement: Outlet fields are classified as OPERATIONAL or JURIDICAL

Fields SHALL be classified in `OutletField` enum with an `isOperational()` predicate:

- **OPERATIONAL** (owner of MOBILE/VIRTUAL may edit with `outlet.self.write`): `DISPLAY_NAME`, `ADDRESS`, `PHONE_CONTACT`, `CURRENT_LOCATION_GPS`, `OPENING_HOURS`
- **JURIDICAL** (TENANT_ADMIN only): `CODE`, `KIND`, `OWNER_AGENT_ID`, `CURRENCY`, `TAX_ID`, `RECEIPT_FOOTER`, `RECEIPT_HEADER_LOGO`

#### Scenario: Owner agent cannot update juridical field

- **WHEN** `UpdateOutletCommand` is handled with a juridical field and caller is the owner agent (not TENANT_ADMIN)
- **THEN** 403 `OUTLET_FIELD_FORBIDDEN` is returned

#### Scenario: TENANT_ADMIN can update any field

- **WHEN** `UpdateOutletCommand` is handled by a TENANT_ADMIN
- **THEN** any field in the map is applied; CRITICAL audit if juridical field changed

#### Scenario: FIXED outlet is not self-writable

- **WHEN** `UpdateOutletCommand` targets a FIXED outlet and caller is not TENANT_ADMIN
- **THEN** 403 `OUTLET_NOT_OWNABLE` is returned

#### Scenario: Owner updates operational field

- **WHEN** `UpdateOutletCommand` targets a MOBILE/VIRTUAL outlet, caller is the owner agent with `outlet.self.write`
- **THEN** the operational field is updated and a STANDARD audit entry is created

---

### Requirement: Outlet commands implemented

The following commands SHALL be handled:

| Command                                                  | Caller                | Effect                                       |
| -------------------------------------------------------- | --------------------- | -------------------------------------------- |
| `CreateOutletCommand`                                    | TENANT_ADMIN          | insert all fields                            |
| `UpdateOutletCommand(outletId, Map<OutletField,Object>)` | TENANT_ADMIN or owner | enforced by handler per field classification |
| `ReassignOutletOwnerCommand(outletId, newOwnerAgentId)`  | TENANT_ADMIN          | updates OWNER_AGENT_ID; CRITICAL audit       |
| `ChangeOutletStatusCommand(outletId, status)`            | TENANT_ADMIN          | updates status; CRITICAL audit               |

#### Scenario: ReassignOutletOwnerCommand emits OutletKindOrOwnerChangedEvent

- **WHEN** `ReassignOutletOwnerCommand` is handled
- **THEN** `OutletKindOrOwnerChangedEvent` is published after-commit with old and new owner IDs

---

### Requirement: Outlet queries implemented

The following queries SHALL be handled:

| Query                          | Returns                                     |
| ------------------------------ | ------------------------------------------- |
| `GetMyOutletQuery(agentId)`    | the outlet the agent owns or is assigned to |
| `GetOutletByIdQuery(outletId)` | full outlet view                            |
| `ListTenantOutletsQuery(page)` | paged list                                  |

#### Scenario: GetMyOutletQuery returns correct outlet

- **WHEN** `GetMyOutletQuery` is received for an agent who owns a MOBILE outlet
- **THEN** that outlet is returned

---

### Requirement: Outlet events emitted after-commit

```java
OutletCreatedEvent(OutletId id, TenantId tenantId, OutletKind kind, AppUserId ownerAgentId)
OutletUpdatedEvent(OutletId id, TenantId tenantId, Set<OutletField> changedFields, boolean juridicalTouched)
OutletKindOrOwnerChangedEvent(OutletId id, TenantId tenantId, OutletKind oldKind, OutletKind newKind, AppUserId oldOwnerId, AppUserId newOwnerId)
OutletStatusChangedEvent(OutletId id, TenantId tenantId, OutletStatus oldStatus, OutletStatus newStatus)
```

`OutletKindOrOwnerChangedEvent` is consumed by `core.session` to abort previous owner's open sessions.
`OutletStatusChangedEvent` to non-ACTIVE is consumed by `core.session` to abort all sessions on that outlet.

#### Scenario: OutletStatusChangedEvent triggers session abort

- **WHEN** `OutletStatusChangedEvent(newStatus != ACTIVE)` is published
- **THEN** the `SalesSessionAbortOnOutletStatusChangedListener` sends `AbortSalesSessionsForOutletCommand`

---

### Requirement: Schema additions applied in-place to existing migration

The following DDL SHALL be added to the existing migration file that creates `outlet`:

```sql
ALTER TABLE outlet
  ADD COLUMN IF NOT EXISTS kind            varchar(16) NOT NULL DEFAULT 'FIXED',
  ADD COLUMN IF NOT EXISTS owner_agent_id  uuid        NULL;

ALTER TABLE outlet ADD CONSTRAINT chk_outlet_kind
  CHECK (kind IN ('FIXED','MOBILE','VIRTUAL'));

ALTER TABLE outlet ADD CONSTRAINT chk_outlet_kind_owner CHECK (
  (kind = 'FIXED' AND owner_agent_id IS NULL) OR
  (kind IN ('MOBILE','VIRTUAL') AND owner_agent_id IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS ix_outlet_owner_agent
  ON outlet (tenant_id, owner_agent_id)
  WHERE deleted_at IS NULL AND owner_agent_id IS NOT NULL;
```

#### Scenario: Schema validates after migration

- **WHEN** a fresh DB is created and all migrations run
- **THEN** `ddl-auto=validate` passes for the `outlet` table

#### Scenario: RLS not broken

- **WHEN** existing RLS policy on `outlet` is tested after migration
- **THEN** tenant isolation still applies
