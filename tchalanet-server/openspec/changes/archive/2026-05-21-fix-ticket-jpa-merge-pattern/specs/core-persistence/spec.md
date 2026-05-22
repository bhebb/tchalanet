## ADDED Requirements

### Requirement: Sensitive aggregate updates avoid detached JPA merge

Core persistence adapters that update a sensitive existing aggregate SHALL NOT rebuild a
fresh JPA entity from the domain aggregate and call `save` as the update mechanism.
Sensitive aggregates include rows that own tenant scope, lifecycle status, money or
accounting state, audit/version state, operational blocks, session state, or settlement
state.

Existing sensitive rows SHALL be updated by loading the managed JPA entity in the same
transaction and mutating only allowed fields, or by explicit SQL guarded by tenant,
status, version, natural-key, or idempotency predicates. The create path MAY map a
domain aggregate to a new JPA entity when the row cannot already exist.

Mutator coverage SHALL include command-level integration tests for the real commands
that call the writer, not only unit tests of the mutator or persistence adapter.

#### Scenario: Existing ticket update uses managed mutation

- **GIVEN** an existing ticket with audited parent, line, and charge rows
- **WHEN** the ticket is marked printed, approved, cancelled, resulted, or settled
- **THEN** the persistence adapter loads the managed ticket aggregate row first
- **AND** it mutates only allowed lifecycle, print, money, settlement, line, or charge fields
- **AND** it does not transplant `tenant_id`, `version`, `created_at`, or `created_by`
- **AND** Hibernate dirty checking owns version increments and audit updates

#### Scenario: Ticket command tests cover mutator field coverage

- **GIVEN** command handlers that call `TicketWriterPort#save`
- **WHEN** `RecordTicketPrintCommand`, `ApproveTicketSaleCommand`,
  `CancelTicketCommand`, an exposed void-ticket command, or offline promotion/sync
  creates or updates a ticket
- **THEN** integration tests execute those real command handlers
- **AND** the tests assert the expected mutable fields changed
- **AND** the tests assert tenant, ticket id, initial outlet, initial terminal, seller,
  sales session, draw, business date, codes, creation audit, and currency did not change

#### Scenario: Existing sales session update uses managed mutation

- **GIVEN** an existing sales session row
- **WHEN** the session is closed, finalized, or has closing cash state updated
- **THEN** the persistence adapter loads the managed sales session row first
- **AND** tenant, outlet, terminal, opener, opened time, and business date are asserted immutable
- **AND** only session lifecycle and cash-close fields are mutated

#### Scenario: Sensitive row missing on update fails

- **GIVEN** a domain aggregate carries an existing id for payout, terminal, outlet, session,
  draw, or limit assignment
- **WHEN** the persistence adapter cannot find the row for update
- **THEN** the adapter fails with a not-found or conflict error
- **AND** it does not create a replacement row with the caller-supplied existing id

### Requirement: Guarded SQL is an explicit alternative to managed mutation

Core persistence adapters SHALL use explicit SQL only for bulk lifecycle transitions, result
ingestion, idempotent replay, and append-only writes when the SQL encodes the domain
concurrency guard directly.

For tables with a `version` column, guarded SQL updates SHALL increment the version when
they mutate the row. For final or irreversible states, guarded SQL SHALL preserve the
state unless the command explicitly authorizes an override.

#### Scenario: Draw lifecycle bulk update uses SQL guards

- **GIVEN** scheduled draws due to open or close
- **WHEN** a bulk draw lifecycle adapter updates them through SQL
- **THEN** the SQL scopes the update to the intended ids and tenant context
- **AND** it only changes rows in compatible lifecycle states
- **AND** it increments `version` when the table version column is mutated

#### Scenario: DrawResult final state is preserved

- **GIVEN** a draw result already in `CONFIRMED` or `OVERRIDDEN`
- **WHEN** result ingestion replays the same natural key without force
- **THEN** the writer does not overwrite final result payload or status
- **AND** no detached JPA merge is used to apply the replay

#### Scenario: Ledger entries are append-only

- **GIVEN** a ledger entry id already exists
- **WHEN** a writer attempts to persist another ledger entry with the same id
- **THEN** the writer rejects the operation or the database unique constraint fails
- **AND** the existing ledger entry is never updated through JPA merge

### Requirement: Sensitive persistence convention is enforced

The backend SHALL include a convention or architecture test that flags update-capable
core persistence adapters which call `save(mapper.toEntity(...))` for sensitive
entities. The test MAY allowlist create-only mappers, append-only inserts, and guarded
SQL writers when the allowlist explains why the path is safe.

#### Scenario: Unsafe rebuild-save adapter is flagged

- **GIVEN** a core adapter writes a `BaseEntity` or `BaseTenantEntity` aggregate with
  inherited `@Version`
- **WHEN** the adapter maps domain to a fresh entity and immediately calls repository
  `save` for a path that can update an existing row
- **THEN** the convention test fails
- **AND** the adapter must be changed to managed mutation or guarded SQL

#### Scenario: Guarded SQL writer is allowed

- **GIVEN** a core writer uses SQL with explicit tenant, status, version, natural-key, or
  idempotency guards
- **WHEN** the convention test evaluates the writer
- **THEN** the writer may be allowlisted
- **AND** the allowlist documents the guard that prevents detached-merge corruption
