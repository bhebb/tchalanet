# complete-navigation-shell-error-alignment

## ADDED Requirements

### Requirement: Navigation contracts are centralized in libs/api

The web workspace SHALL define `ActionItem` and `NavigationDestination` in
`libs/api/src/lib/contracts` and SHALL export them from `libs/api/src/index.ts`.

#### Scenario: Library consumer imports navigation contracts

- **WHEN** a web UI module needs shared navigation typing
- **THEN** it imports from `@tch/api` public exports
- **AND** contract ownership remains in `libs/api/src/lib/contracts`

### Requirement: Shared navigation helpers are canonical

The workspace SHALL provide shared helpers for action label and destination resolution:
`actionText`, `actionRoute`, `actionHref`, `isExternalAction`, `isRouteAction`.

#### Scenario: Component receives a route action

- **WHEN** `item.destination.kind` is `route`
- **THEN** `actionRoute` returns the route value
- **AND** `isRouteAction` is true

#### Scenario: Component receives an external action

- **WHEN** `item.destination.kind` is `url`
- **THEN** `actionHref` returns the URL value
- **AND** `isExternalAction` is true

### Requirement: Navigation UI uses ActionItem without redesign

`brand`, desktop `nav`, `overlay-nav`, and `sidebar-nav` SHALL consume `ActionItem` and shared
helpers for route-vs-url behavior while preserving existing visual design.

#### Scenario: Migrated navigation components render mixed destinations

- **WHEN** a component receives both route and URL actions
- **THEN** internal actions use route navigation
- **AND** external actions use URL-safe rendering
- **AND** no new `TchLink` usage is introduced in these components

### Requirement: Shell runtime integration uses resolved shell contract

`PublicShell` SHALL render from `shell.header` and `shell.footer`.
`PrivateShell` SHALL render from `shell.topAppBar` and `shell.navigationDrawer`.

The runtime SHALL NOT depend on `shell.header.component = PrivateShell` or
`dynamic.widgets['shell.header']` for private shell composition.

#### Scenario: Private runtime page is rendered

- **WHEN** a private page response includes `topAppBar` and `navigationDrawer`
- **THEN** private chrome is rendered from those fields
- **AND** sidenav is available for tenant admin, superadmin, and cashier roles

### Requirement: PageModel remains content-only

PageModel runtime scope SHALL remain limited to `content.layout.rows[].columns[].widgets`.

#### Scenario: Page content is rendered

- **WHEN** runtime page content is rendered
- **THEN** shell composition is outside PageModel content structures
- **AND** PageModel does not own header/footer/top-app-bar/navigation-drawer composition

### Requirement: Error components are separated by level

The web UI SHALL provide page-level, section-level, and field-level error boundaries:

- `tch-page-error` for page failures;
- `tch-error-panel` for section/widget/card failures;
- standardized field errors with `tch-field-error` or `mat-error`.

`NotFoundComponent` SHALL use the page-level error boundary.

#### Scenario: Not found route is displayed

- **WHEN** a not-found page is rendered
- **THEN** the UI uses `tch-page-error`
- **AND** it does not reuse section-level error presentation by default

### Requirement: Style and theme documentation align with conventions docs

Shared style primitives SHALL be documented in `libs/ui/styles/src/lib/README.md` for library usage.
Theme and style decisions SHALL be documented in:
`docs/conventions/theme.md` and `docs/conventions/style.md`.

#### Scenario: Contributor seeks style/theme guidance

- **WHEN** a contributor checks source documentation
- **THEN** they find library usage guidance in `libs/ui/styles/src/lib/README.md`
- **AND** runtime theme/style conventions in `docs/conventions/*`
- **AND** no dependency exists on `libs/ui/theme/README.md`
