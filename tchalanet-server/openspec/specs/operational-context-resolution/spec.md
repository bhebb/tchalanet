# operational-context-resolution Specification

## Purpose
TBD - created by archiving change refactor-operational-context-identity. Update Purpose after archive.
## Requirements
### Requirement: Seller operational context resolution

Seller operational context resolution SHALL validate runtime operation frames before POS-sensitive actions such as sell, offline sale sync, or payout execution.

#### Scenario: Seller context resolves successfully

- **GIVEN** a request context with tenant and actor facts
- **AND** the actor is allowed to operate on the terminal/outlet/session frame
- **WHEN** seller operational context is resolved
- **THEN** the resolver SHALL return a value object or result containing tenant id, actor user id, terminal id, outlet id, and session id when required
- **AND** it SHALL NOT return JPA entities

### Requirement: Resolver composition uses public APIs

Operational context resolution SHALL compose facts through public APIs only.

#### Scenario: Resolver reads cross-module facts

- **GIVEN** the resolver needs identity, permissions, terminal, outlet, or session state
- **WHEN** it queries those facts
- **THEN** it SHALL use `platform.identity.api`, `platform.accesscontrol.api`, `core.terminal.api`, `core.outlet.api`, and `core.session.api`
- **AND** it SHALL NOT import internal packages from those modules

### Requirement: Resolver does not mutate business state

Operational context resolution SHALL validate the operation frame without mutating business state or bypassing RLS.

#### Scenario: Invalid session frame

- **GIVEN** a seller action requires an open session
- **AND** the resolved session is missing or closed
- **WHEN** seller operational context is resolved
- **THEN** the resolver SHALL fail with a problem-compatible application exception
- **AND** no sales mutation SHALL run

### Requirement: Business invariants remain in core

Business use cases SHALL remain responsible for business invariants after operational context has been resolved.

#### Scenario: Sell command executes after context resolution

- **GIVEN** seller operational context has been resolved
- **WHEN** the sell command executes
- **THEN** core sales SHALL still enforce sales-specific invariants
- **AND** the resolver SHALL NOT move those invariants into platform identity

### Requirement: Attach Early Validate Late

Operational context SHALL be attached early when present, but transactional resources SHALL be validated late by the use case that needs them.

The attached context SHALL NOT be considered business proof.

#### Scenario: POS action receives context

- **GIVEN** a request carries terminal, outlet and sales session ids
- **WHEN** the runtime context is created
- **THEN** the ids may be attached as operational context
- **AND** terminal, outlet, session, seller assignment and action eligibility are not validated by the context filter or parser.

#### Scenario: Sensitive action executes

- **GIVEN** a sensitive POS handler is invoked
- **WHEN** it requires operational context
- **THEN** it checks for a present and sufficiently trusted role-specific context
- **AND** it invokes domain validators for terminal, outlet, session and action-specific invariants inside or immediately before the transaction.

### Requirement: No Operational Bounded Context In V1

V1 SHALL NOT introduce `core.operationalcontext` or `platform.operationalcontext`.

The owning domains SHALL keep orchestration of terminal, outlet and session validation until real duplication justifies a future change.

#### Scenario: Sales validates POS resources

- **GIVEN** a sales use case needs terminal, outlet and session checks
- **WHEN** the use case executes
- **THEN** the sales validator or caller orchestrates the required queries
- **AND** it does not delegate to a new operational context bounded context.

#### Scenario: Payout or offline sync needs different policy

- **GIVEN** payout or offline sync needs operational validation
- **WHEN** their validators execute
- **THEN** each domain can apply its own policy variation
- **AND** common does not centralize those policies.

### Requirement: Role-Aware Operational Context

Operational context SHALL distinguish the operational role from the authenticated actor.

The supported roles SHALL include seller, admin, super-admin, system and none.

#### Scenario: Seller acts through POS

- **GIVEN** a seller performs an online POS sale
- **WHEN** the handler requires seller operational context
- **THEN** the context includes terminal id, outlet id, sales session id, seller user id, source and trust level.

#### Scenario: Admin acts in management mode

- **GIVEN** a tenant admin performs a management action
- **WHEN** no POS operation is requested
- **THEN** no seller context is implied
- **AND** the admin does not automatically become a seller.

#### Scenario: Admin explicitly selects POS mode

- **GIVEN** a tenant admin has an active admin POS selection
- **WHEN** a POS-sensitive use case accepts admin POS mode
- **THEN** the attached operational context uses the admin role and `ADMIN_SELECTION`
- **AND** the target domain still validates terminal, outlet, session, permission and action eligibility.

### Requirement: Seller User May Differ From Actor

The POS operational context SHALL carry `sellerUserId` separately from the current actor user id.

#### Scenario: Online POS sale

- **GIVEN** a seller performs an online POS sale
- **WHEN** the operational context is attached
- **THEN** `sellerUserId` may equal the current actor user id.

#### Scenario: Offline sale replay

- **GIVEN** an offline sale is replayed by sync infrastructure
- **WHEN** the runtime actor is system or a sync service
- **THEN** `actorUserId` identifies the executor
- **AND** `sellerUserId` identifies the cashier who produced the original sale.

### Requirement: Operational Role Matrix

Sensitive operations SHALL follow the normative role matrix defined by this change.

Domains MAY add stricter rules, but SHALL NOT loosen the matrix without a new OpenSpec change.

#### Scenario: Seller sells a ticket

- **GIVEN** a sell ticket operation
- **WHEN** the actor is a seller with a trusted POS context
- **THEN** the operation may continue to domain validation.

#### Scenario: Admin performs POS payout

- **GIVEN** a POS payout operation
- **WHEN** the actor is an admin without explicit POS selection
- **THEN** the operation is rejected before transactional payout validation.

#### Scenario: Super-admin performs tenant POS operation

- **GIVEN** a super-admin calls a tenant POS operation
- **WHEN** the request has a valid tenant override and explicit POS context
- **THEN** the operation may continue to domain validation
- **AND** the super-admin does not bypass domain invariants.

### Requirement: Temporary Flat Context Bridge

The current flat operational context SHALL remain only as a compatibility bridge during migration.

New sensitive flows SHALL prefer typed role-aware helpers once available.

#### Scenario: Existing caller still uses flat context

- **GIVEN** a migrated build still contains existing callers of the flat context
- **WHEN** the bridge is present
- **THEN** those callers continue to compile
- **AND** the bridge is marked for migration instead of becoming the preferred API.

#### Scenario: New caller is added after typed context exists

- **GIVEN** typed operational context helpers exist
- **WHEN** a new sensitive handler is implemented
- **THEN** it uses the typed helpers
- **AND** ArchUnit or equivalent checks prevent unmanaged growth of flat bridge usage.

