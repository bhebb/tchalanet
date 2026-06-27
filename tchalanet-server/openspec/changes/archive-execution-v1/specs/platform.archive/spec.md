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

#### Scenario: Ticket data grows

- **GIVEN** `ticket` and `ticket_line` keep growing
- **WHEN** new data is written
- **THEN** it lands in the current monthly partition
- **AND** old partitions can be archived and cleaned independently
- **AND** dashboard reads use projections, not direct `ticket_line` aggregation

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

### Requirement: Module ownership stays intact

`platform.archive` SHALL orchestrate archive runs without owning core persistence.

#### Scenario: Ticket archive provider

- **GIVEN** ticket data belongs to `core.sales`
- **WHEN** `platform.archive` needs ticket export
- **THEN** it calls the `ArchiveDatasetProvider` API
- **AND** ticket-specific SQL and DTO assembly stay in the owning module
- **AND** `platform.archive.internal` does not import `core.*.internal`
