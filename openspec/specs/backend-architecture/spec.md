# Specification: Backend Feature Architecture Alignment

## Purpose

Define backend feature architecture guardrails so feature modules remain UI-oriented BFF orchestration surfaces and any read-model exceptions stay explicit, bounded, and documented.

## Requirements

### Requirement: Features remain orchestration surfaces

Features SHALL NOT own domain write-side command handlers, domain event listeners, or persistent domain aggregates.

#### Scenario: Feature needs a domain write

- **WHEN** a feature endpoint triggers a domain write or state transition
- **THEN** the endpoint SHALL dispatch a command owned by the relevant core domain
- **AND** the command handler SHALL live under `core/<domain>/application/command/handler`

### Requirement: Feature exceptions are documented and bounded

Any feature-level exception SHALL be documented in the feature near-code documentation with a rationale, owner, allowed dependencies, and migration path.

#### Scenario: Public BFF uses a SQL projection

- **WHEN** a public BFF uses a direct projection repository for performance
- **THEN** the feature documentation SHALL state that the repository is read-only
- **AND** it SHALL NOT introduce writes or business invariants in the feature
