# admin-generated-draws-resilience Specification

## Purpose
TBD - created by archiving change generated-draws-admin-resilience-v1. Update Purpose after archive.
## Requirements
### Requirement: Generated-draw mutations preserve normalized feedback

The generated-draw admin page SHALL keep list, lifecycle, and result-save failures attached to
their owning surface through the shared async error contract.

#### Scenario: Lifecycle action failure

- **GIVEN** a lifecycle action is started for a generated draw
- **WHEN** the mutation fails
- **THEN** the row or list surface shows normalized feedback
- **AND** unrelated rows remain usable.

#### Scenario: Result drawer save failure

- **GIVEN** the result drawer is open
- **WHEN** result saving fails
- **THEN** the drawer shows normalized mutation feedback
- **AND** the list shell does not replace it with raw error prose.

