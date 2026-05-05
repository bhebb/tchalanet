# project-documentation Specification

## ADDED Requirements

### Requirement: Markdown Inventory

The project SHALL provide a generated inventory of Markdown documentation files across the repository.

#### Scenario: Count Markdown files
- **WHEN** the documentation inventory script is executed
- **THEN** it SHALL count all `.md` files outside generated/vendor directories
- **AND** it SHALL group them by project/component.

#### Scenario: Generate inventory output
- **WHEN** the inventory script completes
- **THEN** it SHALL produce a machine-readable inventory file
- **AND** it SHALL produce a human-readable Markdown inventory page.

### Requirement: Documentation Ownership

The project SHALL define a canonical owner for each major documentation category.

#### Scenario: Identify canonical source
- **WHEN** a documentation file is classified
- **THEN** it SHALL identify whether the file is canonical, summary, duplicate, obsolete, link-only, or unknown.

#### Scenario: Component docs remain near code
- **WHEN** a document describes implementation details for a component
- **THEN** it SHOULD remain in that component
- **AND** MkDocs SHOULD link to it instead of duplicating it.

### Requirement: MkDocs as Published Portal

`tchalanet-docs` SHALL serve as the published documentation portal.

#### Scenario: Link to component docs
- **WHEN** MkDocs documents a component
- **THEN** it SHALL provide a curated summary
- **AND** link to canonical docs near the owning component.

#### Scenario: Avoid duplication
- **WHEN** a long implementation document already exists near code
- **THEN** MkDocs SHALL NOT copy the full content unless explicitly justified.

### Requirement: OpenSpec Documentation Map

MkDocs SHALL describe the OpenSpec layout without centralizing all component OpenSpecs.

#### Scenario: Global OpenSpec stays light
- **WHEN** OpenSpec context is referenced from MkDocs
- **THEN** MkDocs SHALL document that global context packs are summaries/routers
- **AND** component OpenSpecs remain in their component/project.

#### Scenario: Active changes are discoverable
- **WHEN** a developer looks for active OpenSpec changes
- **THEN** MkDocs SHALL provide a page linking to active global and component OpenSpec workspaces.

### Requirement: Safe Cleanup

Documentation cleanup SHALL be staged and reviewable.

#### Scenario: Obsolete doc found
- **WHEN** a doc is marked obsolete
- **THEN** it SHALL be archived or listed for later deletion
- **AND** it SHALL NOT be silently deleted.

#### Scenario: Duplicate doc found
- **WHEN** a likely duplicate is detected
- **THEN** the cleanup plan SHALL identify the canonical source
- **AND** the duplicate SHALL be archived, merged, or converted to a link after review.
