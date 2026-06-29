## ADDED Requirements

### Requirement: Shared web runtime entrypoints

The web workspace SHALL expose reusable runtime capability entrypoints for code that can be shared
by multiple Angular apps without importing from an application `core` folder.

#### Scenario: A feature consumes reusable error helpers

- **WHEN** a page, store, API feedback interceptor, or form needs normalized error copy or
  page/section/field routing helpers
- **THEN** it imports those helpers from `@tch/web/errors`
- **AND** it does not import reusable error helpers from `apps/*/src/app/core/api`.

#### Scenario: An app wires runtime capabilities

- **WHEN** an app configures auth, i18n, shell feedback, or API providers
- **THEN** app `core` owns only app-specific provider wiring and composition choices
- **AND** reusable capability contracts live behind `@tch/web/auth`, `@tch/web/i18n`,
  `@tch/web/shell`, `@tch/web/core`, or `@tch/api`.

### Requirement: Backend API boundary remains separate from web runtime capabilities

The web workspace SHALL keep backend transport contracts and low-level HTTP mapping in `@tch/api`.

#### Scenario: A reusable web error model needs backend diagnostics

- **WHEN** frontend error helpers need backend status, code, trace id, request id, error id, or span id
- **THEN** they consume normalized `@tch/api` models
- **AND** they do not redefine `ApiResponse`, `ProblemDetail`, or `TchBackendClient` in `@tch/web/errors`.
