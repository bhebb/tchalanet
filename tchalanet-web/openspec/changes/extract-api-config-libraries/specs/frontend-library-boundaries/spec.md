## ADDED Requirements

### Requirement: Transverse API infrastructure has a stable library boundary

The Web workspace SHALL expose common HTTP contracts, helpers, error mapping, and interceptors
through `libs/api` without moving surface-specific clients into that library.

#### Scenario: A surface client consumes common API infrastructure

- **WHEN** a Web surface client needs `ApiResponse` or response unwrapping
- **THEN** it imports those capabilities from the `api` public entrypoint
- **AND** the surface-specific client remains owned by its surface or domain library

### Requirement: Runtime settings and feature flags have a stable library boundary

The Web workspace SHALL expose runtime settings and provider-agnostic feature flags through
`libs/shared-config`.

#### Scenario: Application bootstrap consumes runtime configuration

- **WHEN** application bootstrap loads settings or resolves a feature flag
- **THEN** it consumes the `shared-config` public entrypoint
- **AND** cross-cutting auth, i18n, theme, and PageModel orchestration remains outside the library

### Requirement: PageModel precedes widgets and Web surfaces

The extraction sequence SHALL establish the PageModel boundary before extracting concrete widgets,
and SHALL keep routed dashboards and shells in the Web surface boundary.

#### Scenario: A dashboard uses a dynamic widget

- **WHEN** a routed dashboard renders PageModel content
- **THEN** the dashboard remains owned by `web`
- **AND** the PageModel renderer remains owned by `page-model`
- **AND** the concrete widget and registry remain owned by `widgets`
