# Spec: Prerequisites

## ADDED Requirements

### Requirement: PageModelTemplate sources are consolidated before runtime providers

Before dashboard/overview runtime implementation starts, `PageModelTemplate` documents SHALL be re-seeded with consolidated provider sources.

Allowed sources:

- `json_file`
- `public_home`
- `public_draw_results`
- `tenant_admin_dashboard`
- `cashier_dashboard`
- `platform_admin_dashboard`

#### Scenario: Re-seeded dashboard template

- **GIVEN** a dashboard template has multiple dynamic widgets
- **WHEN** it is re-seeded for V1
- **THEN** role-specific widgets use the grouped source
- **AND** existing `widgetId` values remain stable.

### Requirement: Features do not import PageModel internals

Feature code SHALL NOT import `core.pagemodel.internal.*`.

#### Scenario: PageModel onboarding service compiles

- **WHEN** `PageModelOnboardingService` needs PageModel reads/writes
- **THEN** it uses `core.pagemodel.api.*` contracts
- **AND** it does not import `core.pagemodel.internal.*`.

### Requirement: Widget registry exists

A widget registry SHALL define legal widget ids by `schema_version` and source.

#### Scenario: Template declares unknown widget

- **GIVEN** a template declares a `widgetId` not known by registry
- **WHEN** the template is seeded or resolved
- **THEN** the system reports an explicit error
- **AND** does not silently render an empty widget.
