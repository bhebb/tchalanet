# platform.archive Spec

## ADDED Requirements

### Requirement: Archive execution is verified before cleanup

`platform.archive` SHALL export eligible data before any hot partition is detached, truncated or
dropped.

#### Scenario: Monthly archive succeeds

- **GIVEN** a closed monthly period older than retention
- **WHEN** an archive run executes
- **THEN** it creates an idempotent `archive_run`
- **AND** exports rows through the owning module `ArchiveDatasetProvider`
- **AND** writes compressed archive objects
- **AND** verifies checksum and row count
- **AND** writes lookup index entries for searchable entities
- **AND** marks objects `VERIFIED`
- **AND** only then allows a separate cleanup plan

#### Scenario: Verification fails

- **GIVEN** archive export completes with checksum or row-count mismatch
- **WHEN** cleanup is evaluated
- **THEN** no partition cleanup is allowed
- **AND** hot data remains available

### Requirement: High-volume tables are partitioned by operational time

High-volume append-heavy tables SHALL be partitioned monthly by their operational timestamp:
`sold_at`, `occurred_at`, `created_at` or equivalent.

Tables whose current primary keys, unique constraints or foreign keys do not include the intended
partition key SHALL NOT be force-converted to PostgreSQL RANGE partitions by ad-hoc DDL. They SHALL
either be created partitioned from the start with compatible keys, migrated through a dedicated
schema redesign, or cleaned through verified archive plus guarded batched purge.

#### Scenario: Ticket data grows

- **GIVEN** `ticket` and `ticket_line` keep growing
- **WHEN** new data is written
- **THEN** it lands in the current monthly partition
- **AND** old partitions can be archived and cleaned independently
- **AND** dashboard reads use projections, not direct `ticket_line` aggregation

#### Scenario: Existing id-only foreign keys block safe partition conversion

- **GIVEN** `sales_ticket`, `draw` or `draw_result` is referenced by id-only foreign keys
- **WHEN** an operator asks to partition the table by month
- **THEN** the system documents that PostgreSQL unique constraints must include the partition key
- **AND** no migration drops FK integrity silently
- **AND** cleanup uses verified archive plus batched deletes until a partition-compatible schema is designed

#### Scenario: Business lookup uses business date

- **GIVEN** a report filters by `business_date`
- **WHEN** it reads hot data
- **THEN** `business_date` is indexed
- **AND** it is not the physical partition key unless it is also the safest operational timestamp

### Requirement: Archived records remain legally retrievable

Archive SHALL preserve enough lookup metadata to retrieve disputed or legally requested records.

#### Scenario: Archived ticket dispute

- **GIVEN** a ticket is no longer in hot tables
- **WHEN** an authorized admin searches by ticket id or public code
- **THEN** the service checks `archive_lookup_index`
- **AND** reads the archive object through backend storage
- **AND** returns an archived DTO marked `archived=true`
- **AND** does not expose the storage URI

#### Scenario: Legal hold blocks purge

- **GIVEN** a legal hold exists for a ticket, draw, audit entity, dataset or period
- **WHEN** archive cleanup evaluates matching data
- **THEN** purge, truncate and partition drop are refused
- **AND** the refusal is visible in the archive run or cleanup result

### Requirement: Restore is exceptional and audited

Read-from-archive SHALL be the normal retrieval mode. Restore tables SHALL be used only for larger
investigations.

#### Scenario: Platform restore

- **GIVEN** a SUPER_ADMIN requests restore with a reason
- **WHEN** the restore is approved
- **THEN** rows are loaded into `platform_archive_restore_*` tables
- **AND** restore rows have a TTL
- **AND** the action emits `ARCHIVE_RESTORE`

### Requirement: Emergency purge is explicit and scoped

Emergency purge SHALL be documented separately from normal cleanup and SHALL distinguish business
data from operational technical metadata.

#### Scenario: Spring Batch metadata threatens DB health

- **GIVEN** Spring Batch metadata grows enough to threaten DB operations
- **WHEN** an operator purges old completed batch executions
- **THEN** running jobs are excluded
- **AND** FK child tables are deleted before parent tables
- **AND** business domain tables are not touched

#### Scenario: Ticket lines threaten DB health

- **GIVEN** `sales_ticket_line` grows enough to threaten DB operations
- **WHEN** emergency cleanup is considered
- **THEN** archive verification and legal hold checks are required first
- **AND** child rows are purged in bounded batches
- **AND** ticket headers remain online unless a separate legal/business approval exists

#### Scenario: Ticket hot-table purge is guarded

- **GIVEN** ticket, line and charge archive objects exist for a period
- **WHEN** a SUPER_ADMIN requests ticket purge
- **THEN** the platform first returns a dry-run plan
- **AND** DELETE mode is refused unless archive objects are VERIFIED and row counts match hot rows
- **AND** DELETE mode is refused when a legal hold overlaps the period
- **AND** DELETE mode removes `sales_ticket_charge`, then `sales_ticket_line`, then `sales_ticket`
  in bounded batches

#### Scenario: Draw and revision purge is guarded

- **GIVEN** draw, draw_result or entity_revision archive objects exist for a period
- **WHEN** a SUPER_ADMIN requests domain purge
- **THEN** the platform first returns a dry-run plan
- **AND** DELETE mode is refused unless the matching archive object is VERIFIED and row counts match
- **AND** DELETE mode is refused when a legal hold overlaps the period
- **AND** `draw` purge is refused while tickets still reference matching draws
- **AND** `draw_result` purge is refused while draws still reference matching results
- **AND** Envers purge removes `_aud` rows before `revinfo`

### Requirement: Module ownership stays intact

`platform.archive` SHALL orchestrate archive runs without owning core persistence.

#### Scenario: Ticket archive provider

- **GIVEN** ticket data belongs to `core.sales`
- **WHEN** `platform.archive` needs ticket export
- **THEN** it calls the `ArchiveDatasetProvider` API
- **AND** ticket-specific SQL and DTO assembly stay in the owning module
- **AND** `platform.archive.internal` does not import `core.*.internal`
