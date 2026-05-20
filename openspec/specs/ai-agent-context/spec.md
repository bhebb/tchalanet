# ai-agent-context Specification

## Purpose

This capability defines how AI-agent instruction and configuration files are structured, scoped, and maintained across the monorepo. It governs file inventory, routing, bounded context loading, and safe cleanup of obsolete agent instructions.

## Requirements

### Requirement: AI-Agent File Inventory

The project SHALL provide an inventory of AI-agent instruction and configuration files.

#### Scenario: Scan AI files

- **WHEN** the AI-agent inventory script is executed
- **THEN** it SHALL scan root and component AI instruction files
- **AND** it SHALL exclude generated/vendor directories.

#### Scenario: Generate AI inventory

- **WHEN** the inventory script completes
- **THEN** it SHALL produce a machine-readable inventory
- **AND** it SHALL produce a Markdown inventory page.

### Requirement: Lightweight Global Agent Router

The root AI-agent guidance SHALL remain lightweight.

#### Scenario: Root AGENTS is loaded

- **WHEN** an agent loads root `AGENTS.md`
- **THEN** it SHALL receive routing instructions
- **AND** it SHALL be directed to component docs and OpenSpec context packs
- **AND** it SHALL NOT receive full duplicated component documentation.

### Requirement: Component-Owned Agent Context

Detailed AI-agent instructions SHALL live near the component they describe.

#### Scenario: Backend task

- **WHEN** an agent works on backend code
- **THEN** it SHOULD load backend-specific instructions
- **AND** it SHOULD NOT load frontend, mobile, edge, and infra instructions unless relevant.

#### Scenario: Infra task

- **WHEN** an agent works on infra code
- **THEN** it SHOULD load infra-specific instructions
- **AND** it SHOULD load only the infra technical context pack.

### Requirement: Bounded Context Loading

AI agents SHALL load only the required context for a task.

#### Scenario: Feature work

- **WHEN** an agent starts a feature task
- **THEN** it SHALL load `10-non-negotiables.md`
- **AND** at most one technical pack
- **AND** at most one domain pack
- **AND** the relevant component `AGENTS.md`.

### Requirement: Safe AI Instruction Cleanup

Obsolete AI-agent instructions SHALL be archived before deletion.

#### Scenario: Obsolete instruction found

- **WHEN** an AI-agent instruction file is marked obsolete
- **THEN** it SHALL be archived or listed for later deletion
- **AND** it SHALL NOT be silently deleted.

#### Scenario: Duplicate instruction found

- **WHEN** duplicated AI-agent guidance is found
- **THEN** the cleanup plan SHALL identify the canonical instruction source
- **AND** duplicates SHALL be replaced with links or archived after review.
