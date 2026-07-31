# admin-maryaj-gratis-resilience Specification

## Purpose
TBD - created by archiving change maryaj-gratis-admin-resilience-v1. Update Purpose after archive.
## Requirements
### Requirement: Maryaj gratis required and optional slices are separated

The Maryaj gratis admin page SHALL keep campaign loading page-owned while allowing tenant-game
readiness and campaign enrichment failures to degrade their own sections.

#### Scenario: Optional game configuration failure

- **GIVEN** campaign data loads successfully
- **WHEN** tenant-game configuration fails
- **THEN** the campaign page remains visible
- **AND** the game section shows normalized local degradation feedback.

#### Scenario: Campaign load failure

- **GIVEN** the campaign request fails
- **WHEN** the page renders
- **THEN** the page shows its normalized blocking error state
- **AND** promotion actions are unavailable until the campaign can load.

