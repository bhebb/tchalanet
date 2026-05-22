# Spec: offline-sync

## ADDED Requirements

### Requirement: Offline cutoff validation uses trusted sale instant

Offline sale promotion SHALL validate the trusted sale instant against the draw cutoff instant.

```text
trustedSaleInstant < draw.cutoffAt -> cutoff gate passes
trustedSaleInstant >= draw.cutoffAt -> reject or review
missing/untrusted sale instant -> reject or review
```

#### Scenario: Late sync with trusted pre-cutoff sale

- **GIVEN** a draw cutoff instant `C`
- **AND** an offline submission syncs after `C`
- **AND** its trusted sale instant is before `C`
- **AND** offline grant and signature checks pass
- **WHEN** the submission is evaluated
- **THEN** the cutoff gate passes.

#### Scenario: Late sync with trusted post-cutoff sale

- **GIVEN** a draw cutoff instant `C`
- **AND** an offline submission has trusted sale instant equal to or after `C`
- **WHEN** the submission is evaluated
- **THEN** it is rejected with a cutoff reason.

#### Scenario: Late sync without trusted sale instant

- **GIVEN** a draw cutoff instant `C`
- **AND** an offline submission syncs after `C`
- **AND** no verifiable trusted sale instant is available
- **WHEN** the submission is evaluated
- **THEN** it is rejected or routed to admin review
- **AND** it is not silently promoted.

### Requirement: Device timezone is metadata only

Device timezone SHALL NOT be used as source of truth for cutoff acceptance.

#### Scenario: Device claims Haiti timezone

- **GIVEN** a device submits local sale time and timezone
- **WHEN** offline sync validates cutoff
- **THEN** device timezone is treated as metadata
- **AND** acceptance depends on a trusted instant and server-side draw cutoff.
