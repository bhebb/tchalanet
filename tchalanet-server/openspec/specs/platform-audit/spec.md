# platform-audit Specification

## Purpose

`platform.audit` owns functional audit: business/user action history. It is separate from technical
revision audit such as Envers row revisions.

## Requirements

### Requirement: Functional Audit API

`platform.audit` SHALL expose functional audit through `platform.audit.api`.

#### Scenario: Handler records non-HTTP use case audit

- **GIVEN** a use case can be invoked by batch, event replay, or ops
- **WHEN** the use case requires functional audit independent of HTTP
- **THEN** the handler SHALL call `AuditApi`
- **AND** it SHALL avoid duplicate audit if another layer already records the same business action.

### Requirement: Functional audit is separate from technical revision audit

Functional audit and Envers/technical revision audit SHALL be distinct mechanisms.

#### Scenario: Entity row changes

- **GIVEN** Envers captures a row revision
- **WHEN** the changed row belongs to an audited entity
- **THEN** revision metadata MAY include actor, tenant, request id, and correlation id
- **BUT** a business action requiring functional audit SHALL still record a functional audit entry.

#### Scenario: Functional action without row diff

- **GIVEN** a business action is denied before mutation
- **WHEN** no entity revision is produced
- **THEN** functional audit MAY still record the denied action
- **AND** Envers SHALL NOT be required for that audit entry.

### Requirement: AuditLog annotation records user actions

HTTP user actions SHALL declare functional audit through `@AuditLog` or an equivalent explicit
audit call.

#### Scenario: Controller write succeeds

- **GIVEN** a controller method has `@AuditLog`
- **WHEN** the command succeeds and the transaction commits
- **THEN** audit SHALL be written after commit
- **AND** include actor, tenant, action, entity, outcome, request id, correlation id, and safe details.

#### Scenario: Controller write fails

- **GIVEN** a controller method has `@AuditLog`
- **WHEN** the command throws an exception
- **THEN** failure audit SHALL be written in a separate transaction when policy requires it
- **AND** the original exception SHALL remain the client-visible failure.

### Requirement: Audit failure does not rollback main operation

Audit infrastructure failure SHALL NOT break successful business operations.

#### Scenario: Audit database write fails after main commit

- **GIVEN** the business transaction committed
- **WHEN** audit persistence fails
- **THEN** the failure SHALL be logged/observed
- **AND** the business response SHALL NOT be converted to failure solely because audit failed.

### Requirement: Audit persistence belongs to platform.audit

Functional audit tables and repositories SHALL live under `platform.audit`.

#### Scenario: Common persistence contains audit entity

- **GIVEN** a functional audit entity or repository is added under `common.persistence`
- **WHEN** architecture tests run
- **THEN** the test suite SHALL fail.

### Requirement: Audit endpoints are read-only

Audit HTTP endpoints SHALL allow search/read only, not public writes.

#### Scenario: Admin searches audit log

- **GIVEN** tenant admin requests audit events
- **WHEN** audit events are returned
- **THEN** response SHALL be paginated
- **AND** RLS SHALL restrict tenant scope unless platform override is authorized.
