## ADDED Requirements

### Requirement: OpenSpec-first multiplatform workflow

The repository SHALL require OpenSpec before implementing any new feature or architectural change.
Specs SHALL remain UI-agnostic so that web and mobile consume the same source of truth.

#### Scenario: Start a feature

- **WHEN** a contributor starts a new feature
- **THEN** an OpenSpec change exists before implementation
- **AND** the spec describes shared behavior independently from Angular or Flutter UI details

### Requirement: Backend owns business logic

The backend SHALL remain the single source of truth for business logic.
Web and mobile clients SHALL consume backend contracts without reimplementing business invariants.

#### Scenario: Shared business rule

- **WHEN** the same business rule affects web and mobile
- **THEN** the rule is implemented in the backend
- **AND** clients only adapt presentation and local state

### Requirement: Mandatory validation commands

The repository SHALL expose package scripts for:

- `pnpm ops:status`
- `pnpm ops:check`

Before commit, contributors SHALL run:

- `pnpm ops:check`
- `pnpm lint`
- `pnpm test`
