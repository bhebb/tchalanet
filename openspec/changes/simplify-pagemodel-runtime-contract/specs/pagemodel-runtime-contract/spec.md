# Capability: Simplified PageModel Runtime Contract

## ADDED Requirements

### Requirement: Internal definition and runtime response are separate

The system SHALL keep the internal PageModel definition separate from the frontend runtime response and runtime endpoints SHALL return only a resolved `PageRuntimeResponse`.

Internal definitions MAY contain binding, provider source, fragment keys, and storage metadata.

#### Scenario: Runtime page is serialized

- **WHEN** a resolved PageModel is returned by a runtime endpoint
- **THEN** the response contains resolved shell, content, and widget data
- **AND** it contains no `binding`, `fileKey`, provider source, or storage path

### Requirement: Runtime metadata is minimal

Runtime `meta` SHALL contain only the render-relevant functional identity and compatibility fields `logicalId`, `scope`, `slug`, and `schemaVersion`.

#### Scenario: Template metadata is mapped to runtime

- **WHEN** a template containing `code`, `logicalId`, `scope`, `slug`, `name`, `label`,
  `schemaVersion`, `schema`, `isDefault`, and `level` is assembled
- **THEN** runtime `meta` contains `logicalId`, `scope`, `slug`, and `schemaVersion`
- **AND** runtime output does not contain `code`, `name`, `label`, `schema`, `isDefault`, or `level`

#### Scenario: Page needs rendered title text

- **WHEN** a page or widget displays a title or label
- **THEN** it uses a content i18n key or resolved render content
- **AND** it does not use the administrative template `name` or `label`

### Requirement: Runtime contract uses camelCase

All JSON field names in `PageRuntimeResponse` SHALL use camelCase.

#### Scenario: Runtime metadata and actions are serialized

- **WHEN** runtime metadata, layout labels, or actions are returned
- **THEN** fields use names such as `schemaVersion`, `logicalId`, `labelKey`, `activeMatch`, and
  `reasonKey`
- **AND** no snake_case contract field is present

#### Scenario: Runtime UI identifiers are serialized

- **WHEN** runtime actions, navigation destinations, layout rows, or widget ids are returned
- **THEN** UI identifiers use camelCase names such as `quickActions`, `nextDraws`, and `checkTicket`
- **AND** stable logical ids and i18n keys may retain their established forms

### Requirement: Runtime shell is typed and ready to render

The runtime shell SHALL be discriminated by `shell.type` and SHALL contain its resolved render data.

#### Scenario: Public page shell

- **WHEN** a public page is resolved
- **THEN** `shell.type` is `public`
- **AND** `shell.header` and `shell.footer` are present and ready to render
- **AND** neither section is delivered through widget data

#### Scenario: Private dashboard shell

- **WHEN** a private dashboard is resolved
- **THEN** `shell.type` is `private`
- **AND** `shell.topAppBar` and `shell.navigationDrawer` are present and ready to render
- **AND** the navigation drawer is not hidden inside a header component

### Requirement: Page composition uses one runtime call

The frontend SHALL obtain all PageModel composition data from one PageModel runtime request per page load.

#### Scenario: Public home is loaded

- **WHEN** Angular loads the public home page
- **THEN** one PageModel runtime response provides its shell, layout, widget config, and widget data
- **AND** Angular does not call separate header, footer, hero, or fragment endpoints

#### Scenario: Private dashboard is loaded

- **WHEN** Angular loads a private dashboard
- **THEN** one PageModel runtime response provides its shell, layout, widget config, and widget data
- **AND** the dashboard navigation drawer remains present

### Requirement: Runtime routes describe surfaces

Runtime endpoint routes SHALL describe the rendered surface and SHALL NOT accept PageModel logical ids as route parameters.

#### Scenario: Public page is requested

- **WHEN** an anonymous client requests `GET /api/v1/public/page`
- **THEN** the backend resolves the single public page server-side
- **AND** the client does not provide a logical id

#### Scenario: Tenant dashboard is requested

- **WHEN** an authenticated tenant actor requests `GET /api/v1/tenant/dashboard`
- **THEN** the backend selects the dashboard from `TchRequestContext`
- **AND** the client cannot select an arbitrary private PageModel

#### Scenario: Platform dashboard is requested

- **WHEN** an authorized platform actor requests `GET /api/v1/platform/dashboard`
- **THEN** the backend selects the platform dashboard server-side
- **AND** the client cannot select an arbitrary platform PageModel

### Requirement: Layout V1 remains intentionally limited

Runtime layout SHALL use only rows, columns, span, widget ids, and optional label keys for V1.

#### Scenario: Runtime layout is returned

- **WHEN** a PageRuntimeResponse contains layout
- **THEN** the layout uses rows and columns referencing widget ids
- **AND** it contains no arbitrary CSS, deep nested layout, or backend-driven responsive rules

### Requirement: Widget config and widget data remain distinct

Runtime widget render configuration SHALL be stored in `content.widgets[widgetId]` and volatile resolved widget payloads SHALL be stored in `dynamic.widgets[widgetId]`.

#### Scenario: Widget host renders a dynamic widget

- **WHEN** a layout references a widget id
- **THEN** Angular reads its `type` and resolved `props` from `content.widgets[widgetId]`
- **AND** reads its volatile payload from `dynamic.widgets[widgetId]`
- **AND** performs no binding or provider resolution

#### Scenario: Unknown widget type is received

- **WHEN** Angular receives a widget type with no registered renderer
- **THEN** the failure is contained to that widget
- **AND** the rest of the page remains usable

### Requirement: Navigation uses one destination contract

Navigable runtime actions SHALL use `destination.kind` and `destination.value`.

#### Scenario: Internal route action is rendered

- **WHEN** an action destination kind is `route`
- **THEN** Angular renders it using `routerLink`

#### Scenario: External URL action is rendered

- **WHEN** an action destination kind is `url`
- **THEN** Angular renders it using an external `href`

### Requirement: Runtime failures are sanitized and contained

Provider or fragment failures SHALL NOT expose backend implementation details or discard the full page when they affect only one widget.

#### Scenario: Widget provider fails

- **WHEN** a dynamic widget provider fails
- **THEN** the response may contain a sanitized widget-local error
- **AND** it contains no provider class, storage path, fragment key, or internal exception message

### Requirement: Request notices remain on the API envelope

Request-level notices and services SHALL be returned only by the existing `ApiResponse` envelope and
SHALL NOT be duplicated inside `PageRuntimeResponse`.

#### Scenario: Page runtime succeeds

- **WHEN** a PageRuntimeResponse is returned inside ApiResponse
- **THEN** PageRuntimeResponse contains `dynamic.widgets` and `dynamic.errors`
- **AND** PageRuntimeResponse contains no `notices` or `services`

### Requirement: Public home draw data is lightweight

The `home.draws` runtime payload SHALL expose only the draw fields needed by the public homepage.

#### Scenario: Public home draw slots are returned

- **WHEN** `home.draws` is resolved
- **THEN** each slot may contain identity, schedule, next-result timing, latest-result status, and
  Haiti lot values
- **AND** it contains no provider source payload, source flags, source hash, provider URL, full
  metadata, or history
- **AND** other widgets and the page shell remain available
