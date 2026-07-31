# admin-games-setup-resilience Specification

## Purpose
TBD - created by archiving change tenant-games-setup-resilience-v1. Update Purpose after archive.
## Requirements
### Requirement: Games setup preserves useful data with notices

The tenant games setup page SHALL preserve a successful tenant-game response envelope and render
non-blocking notices locally when an informational setup source is degraded.

#### Scenario: Informational source is unavailable

- **GIVEN** the tenant-game list loads successfully
- **WHEN** an informational setup source returns a notice
- **THEN** the configured games remain visible
- **AND** the page renders the notice at the relevant section boundary.

#### Scenario: Required tenant-game list fails

- **GIVEN** the required tenant-game list cannot load
- **WHEN** the page renders
- **THEN** the page shows a blocking normalized error with retry
- **AND** it does not present stale or fabricated game settings.

