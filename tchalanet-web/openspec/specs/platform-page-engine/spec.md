# platform-page-engine Specification

## Purpose
TBD - created by archiving change platform-page-engine-ops-v1. Update Purpose after archive.
## Requirements
### Requirement: Platform operations page engine

The platform operations area SHALL expose a PageModel engine page for inspecting and editing raw
PageModel JSON through available backend PageModel administration endpoints.

#### Scenario: Platform operator edits a page model JSON document

- **GIVEN** a platform operator is authenticated in the platform portal
- **WHEN** they open `/app/platform/ops/page-engine`
- **THEN** the page lists existing PageModels through a data-access resource
- **AND** selecting a PageModel loads its preview through a data-access resource
- **AND** the operator can edit the JSON and save it through the PageModel update endpoint.

### Requirement: PageModel lifecycle actions follow backend capabilities

The PageModel engine SHALL expose only actions backed by confirmed backend administration methods,
and SHALL not call an archive endpoint until such an endpoint exists.

#### Scenario: Platform operator uses lifecycle actions

- **GIVEN** a PageModel is selected in the page engine
- **WHEN** the operator chooses publish, duplicate, or reset
- **THEN** the page calls the matching backend PageModel administration endpoint
- **AND** the page reloads the list and preview after success.

#### Scenario: Platform operator chooses archive

- **GIVEN** a PageModel is selected in the page engine
- **WHEN** the operator chooses archive
- **THEN** the UI reports that archive is unavailable in the current backend contract
- **AND** no archive HTTP request is sent.

### Requirement: Page engine navigation is shell-owned

The platform private shell navigation SHALL expose the page engine under operations.

#### Scenario: Platform operator opens operations navigation

- **GIVEN** the platform private shell navigation is loaded
- **WHEN** the operator expands operations
- **THEN** a page engine entry is visible
- **AND** it routes to `/app/platform/ops/page-engine`.

