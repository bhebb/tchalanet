# Frontend Library Boundaries

## ADDED Requirements

### Requirement: PageModel runtime is library-owned

The frontend SHALL keep PageModel runtime contracts, API access, rendering, labels, and the widget
registry abstraction owned by `libs/page-model`.

#### Scenario: Render a PageModel in the portal

- **WHEN** the portal renders a resolved PageModel response
- **THEN** it consumes the renderer and contracts through `@tch/page-model`
- **AND** `libs/page-model` does not import concrete widgets

### Requirement: Concrete widgets are independently composed

The frontend SHALL keep concrete PageModel widgets owned by `libs/widgets` and expose a registry
provider for the application composition root.

#### Scenario: Compose the widget registry

- **WHEN** the portal application starts
- **THEN** it provides the concrete widget registry from `@tch/widgets`
- **AND** the PageModel renderer resolves widgets through its registry abstraction

### Requirement: Reusable web presentation is library-owned

The frontend SHALL keep reusable public web shell presentation in `libs/web` without importing
app-owned runtime services.

#### Scenario: Render reusable public shell presentation

- **WHEN** the portal renders reusable public shell elements
- **THEN** it imports them through `@tch/web`
- **AND** app-specific orchestration remains in the portal composition root
