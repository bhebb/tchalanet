# Spec Delta — Audit

## ADDED Requirements

### Requirement: Application audit must use one canonical write path

The system SHALL use `LogAuditEventCommand` as the canonical application audit command.

The canonical application audit write path SHALL be:

```text
@AuditLog or explicit handler trigger
  -> CommandBus.send(LogAuditEventCommand)
  -> AuditLoggingCommandHandler
  -> AuditEventFactory
  -> AuditEventWriterPort
  -> audit_event
```

`RecordAuditEventCommand` SHALL NOT be used as a competing application audit write path.

#### Scenario: HTTP audited action succeeds

- **WHEN** a controller method annotated with `@AuditLog` returns normally
- **AND** the surrounding transaction commits
- **THEN** the system sends `LogAuditEventCommand` after commit
- **AND** exactly one application audit event is persisted for that action

#### Scenario: HTTP audited action rolls back

- **WHEN** a controller method annotated with `@AuditLog` returns normally
- **BUT** the surrounding transaction rolls back
- **THEN** no success application audit event is persisted

#### Scenario: non-HTTP sensitive use-case succeeds

- **WHEN** a sensitive command handler is invoked outside HTTP
- **AND** the use-case requires application audit
- **THEN** the owning handler sends `LogAuditEventCommand`
- **AND** success audit is scheduled after commit

---

### Requirement: Application audit must distinguish success and error transactions

The system SHALL write successful action audits only after commit.

The system SHALL write error audits immediately in an independent transaction.

#### Scenario: success audit after commit

- **WHEN** an audited action succeeds
- **THEN** the audit command is scheduled with `AfterCommit.run(...)`
- **AND** audit is written only if the business transaction commits

#### Scenario: error audit immediately

- **WHEN** an audited action throws an exception
- **THEN** the audit command is sent immediately
- **AND** the audit handler writes with `REQUIRES_NEW`
- **AND** the original business exception remains unchanged

---

### Requirement: Audit failures must never break the main operation

The audit infrastructure SHALL catch and log audit failures.

Audit failures SHALL NOT change business success responses or business exceptions.

#### Scenario: audit send fails after business success

- **WHEN** a business action succeeds
- **AND** sending the audit command fails
- **THEN** the business action remains successful
- **AND** the audit failure is logged

#### Scenario: audit persistence fails after business error

- **WHEN** a business action throws
- **AND** audit persistence fails
- **THEN** the original business exception is preserved
- **AND** the audit failure is logged

---

### Requirement: Audit aspect must not alter controller return values

`AuditLogAspect` SHALL NOT return from its `finally` block.

#### Scenario: audited controller returns a response

- **WHEN** an audited controller returns a non-null response
- **AND** no audit command is built
- **THEN** the original response is returned unchanged

---

### Requirement: Audit entity IDs must be stable strings

Audit `entityId` SHALL be a stable string.

The application audit system SHALL NOT assume `entityId` is a UUID.

#### Scenario: audit entity id is a UUID string

- **WHEN** `idExpression` resolves to a UUID string
- **THEN** the audit event is accepted

#### Scenario: audit entity id is a ticket code

- **WHEN** `idExpression` resolves to a ticket code
- **THEN** the audit event is accepted
- **AND** no UUID parsing is attempted

#### Scenario: audit entity id is a job key

- **WHEN** a batch operation is audited with entity id `RESULTS_EXTERNAL_REFRESH`
- **THEN** the audit event is accepted
- **AND** no UUID parsing is attempted

---

### Requirement: Audit details must be JSONB-safe

Audit details SHALL be normalized to a JSONB-safe structure before persistence.

#### Scenario: details expression returns a map

- **WHEN** `detailsExpression` returns a `Map`
- **THEN** the system stores equivalent JSONB-safe details
- **AND** no lossy `toString()` conversion is used

#### Scenario: details expression returns an object

- **WHEN** `detailsExpression` returns an object
- **THEN** the system converts it through `JsonUtils` or `ObjectMapper`
- **AND** the persisted value is JSONB-safe

#### Scenario: audited action fails

- **WHEN** an audited action throws
- **THEN** details include error metadata
- **AND** the audit event indicates a failed outcome

---

### Requirement: Force operations must be audited by owning handlers

Force operations SHALL be audited by the command handler that owns the use-case.

The system SHALL NOT use a global aspect on `CommandBus.send(..)` to audit force operations.

#### Scenario: force operation succeeds

- **WHEN** a command with `force=true` succeeds
- **THEN** the owning handler schedules a force audit after commit
- **AND** details include `force=true`
- **AND** details include the force reason

#### Scenario: force operation has no reason

- **WHEN** a manual force operation has no reason
- **THEN** the use-case is rejected
- **OR** the operation is not considered valid for manual force

#### Scenario: common layer remains technical

- **WHEN** force audit is implemented
- **THEN** no class under `common` depends on `core.audit`

---

### Requirement: Audit event persistence must store occurredAt

The audit persistence adapter SHALL persist `AuditEvent.occurredAt`.

#### Scenario: audit event is saved

- **WHEN** an audit event is saved
- **THEN** `audit_event.occurred_at` is non-null
- **AND** equals the domain event occurredAt value

---

### Requirement: Audit event must support tenant and platform events

The audit event model SHALL support tenant-scoped and platform/global audit events.

#### Scenario: tenant action is audited

- **WHEN** a tenant-scoped action is audited
- **THEN** the audit event stores tenant id

#### Scenario: platform action is audited

- **WHEN** a platform action is audited
- **THEN** the audit event can be stored without requiring a tenant id
- **AND** platform users can query it according to authorization rules

---

### Requirement: Audit purge must use retention policy and Clock

Audit purge SHALL calculate its threshold using an injected `Clock`.

Audit purge SHALL delete events by `occurred_at`.

#### Scenario: purge removes only expired events

- **GIVEN** `tch.audit.retention-days=90`
- **AND** clock now is `2026-04-29T00:00:00Z`
- **WHEN** purge runs
- **THEN** events with `occurred_at < 2026-01-29T00:00:00Z` are deleted
- **AND** events at or after the threshold remain

#### Scenario: purge logs deletion count

- **WHEN** purge completes
- **THEN** the system logs the number of deleted audit events

---

### Requirement: Canonical audit action coverage must be maintained

The project SHALL maintain a canonical list of application audit actions.

Each sensitive endpoint or use-case SHALL map to one canonical action.

Minimum coverage SHALL include:

- ticket sale, cancel, override, print;
- payout request, approve, reject, execute;
- draw generate, open, close;
- draw result fetch, apply, override;
- outlet, terminal, user, role, limit, commission, tenant theme and feature flag changes;
- super admin override;
- force operations;
- batch job start;
- cache clear;
- tenant create, update and disable.

#### Scenario: adding a sensitive endpoint

- **WHEN** a PR adds a sensitive endpoint
- **THEN** the PR identifies the canonical audit action
- **AND** adds `@AuditLog` or handler-level audit
- **AND** adds test coverage

#### Scenario: action is audited in both controller and handler

- **WHEN** both controller and handler could audit the same action
- **THEN** the implementation chooses one trigger
- **AND** avoids duplicate application audit events

---

### Requirement: Envers must be separate from application audit

Envers SHALL be used for technical row history.

Application audit SHALL be used for business action history.

#### Scenario: business action updates audited entity

- **WHEN** a business action updates an Envers-audited entity
- **THEN** the system may create both an application audit event and an Envers revision
- **AND** each record serves its separate purpose

---

### Requirement: Envers revisions must include execution context when available

Envers `revinfo` SHALL include execution context fields when available.

Minimum fields:

- `tenant_id`
- `user_id`
- `request_id`
- `actor_type`

Optional fields:

- `api_scope`
- `tenant_overridden`

#### Scenario: tenant request modifies audited entity

- **WHEN** an audited entity is modified under tenant context
- **THEN** the revision includes tenant id
- **AND** the revision includes user id when available
- **AND** the revision includes request id when available

#### Scenario: system job modifies audited entity

- **WHEN** an audited entity is modified by a system job
- **THEN** an Envers revision is still created
- **AND** missing request fields do not fail the operation

---

### Requirement: Envers audited entities must be explicit

The project SHALL explicitly decide which entities are `@Audited`.

The project SHALL NOT unintentionally audit every entity through a superclass without review.

#### Scenario: entity is selected for Envers audit

- **WHEN** an entity is marked `@Audited`
- **THEN** a matching `_AUD` table exists
- **AND** Flyway migrations maintain that table

#### Scenario: audited table changes

- **WHEN** a Flyway migration changes an audited table
- **THEN** the corresponding `_AUD` table is updated in the same change
- **AND** `ddl-auto=validate` passes

---

### Requirement: Tenant entity listener must use canonical context

Tenant entity persistence SHALL resolve tenant context from `TchContext` or `TchContextResolver`.

It SHALL NOT use `RequestContextHolder`.

#### Scenario: tenant entity persisted under tenant context

- **WHEN** a tenant-scoped entity is persisted
- **AND** tenant id is not already set
- **THEN** the listener sets tenant id from canonical context

#### Scenario: tenant entity persisted with explicit tenant id

- **WHEN** a tenant-scoped entity is persisted by batch/import
- **AND** tenant id is already set
- **THEN** the listener preserves the existing tenant id

#### Scenario: tenant entity persisted without tenant context

- **WHEN** a tenant-scoped entity is persisted
- **AND** tenant id is not already set
- **AND** no tenant context exists
- **THEN** persistence fails fast

#### Scenario: tenant mismatch on update

- **WHEN** a tenant-scoped entity is updated
- **AND** current tenant context differs from entity tenant id
- **THEN** persistence fails
