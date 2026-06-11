# private-dashboard-widget-rendering

## ADDED Requirements

### Requirement: Private dashboards render through the widget engine

Private dashboards (cashier, tenant admin, platform super-admin) SHALL render through the PageModel
widget engine (`PageModelApi` → `PageModelComponent`/`WidgetHostComponent` → registered widgets) and
SHALL NOT use mock-only feature services.

#### Scenario: Dashboard loads from the backend payload

- **WHEN** a user opens a private dashboard after login
- **THEN** the app renders widgets from the dashboard call's `content.widgets` and `dynamic.widgets`
- **AND** it does not read from a local dashboard mock

#### Scenario: Mock-only dashboard services are removed

- **WHEN** the change is complete
- **THEN** no `*-dashboard.mock.ts` is rendered and no dashboard service returns mock data instead of the runtime payload

#### Scenario: Dashboards are lazy-loaded

- **WHEN** the public page bundle is built
- **THEN** private dashboard code is not included in the public bundle

### Requirement: Widget data binding resolves dynamic payload by path

The widget engine SHALL resolve a value from `dynamic.widgets[id]` when a widget config declares a
data binding `{ source, path }`. The binding SHALL be optional and backward-compatible with widgets
that do not declare one.

#### Scenario: Widget declares a dynamic binding

- **WHEN** a widget config declares `{ source: "dynamic", path }`
- **THEN** the widget reads the value at that path from its `dynamic` payload

#### Scenario: Binding path is missing in the payload

- **WHEN** a declared binding path has no value in the dynamic payload
- **THEN** the widget renders its empty/placeholder state and remains visible

#### Scenario: Widget without a binding keeps current behavior

- **WHEN** an existing public widget has no declared binding
- **THEN** it renders exactly as before this change

### Requirement: Material Symbols are self-hosted as a subsetted variable font

Material Symbols SHALL be served from a self-hosted, subsetted variable font; the app SHALL NOT load
icons from a CDN and SHALL NOT ship the full font.

#### Scenario: Navigation icons render offline

- **WHEN** the private shell loads without external network access to font CDNs
- **THEN** sidebar and top-bar icons render from the self-hosted font

#### Scenario: Icon outside the subset

- **WHEN** an `ActionItem.icon` references a glyph absent from the subset
- **THEN** a fallback glyph renders and the item stays usable

### Requirement: Private dashboard text resolves via i18n keys

Backend-provided keys for private dashboards SHALL resolve through the merged i18n loader, with local
`fr`/`en`/`ht` bundles providing fallback.

#### Scenario: Backend key has a local fallback

- **WHEN** a dashboard widget references a key present in the local bundle but absent from the backend override
- **THEN** the local translation renders

#### Scenario: Key has no translation anywhere

- **WHEN** a key has no resolved value in any source
- **THEN** a stable key-derived fallback renders and the widget stays visible

### Requirement: Private dashboards are tokenized and responsive

Private dashboards and shell SHALL style exclusively through theme tokens (`--tch-*` / `--comp-*`)
and SHALL be responsive from mobile through desktop using shared breakpoints.

#### Scenario: No hardcoded colors

- **WHEN** dashboard or shell styles are reviewed
- **THEN** colors come from `--tch-*` / `--comp-*` tokens, not literal values

#### Scenario: Mobile to desktop layout

- **WHEN** a dashboard is viewed at mobile, tablet, and desktop widths
- **THEN** the layout adapts using `libs/ui/styles` breakpoints without horizontal overflow

### Requirement: Widgets handle loading, empty, and error states

Each private dashboard widget SHALL render a contained loading, empty, and error state and SHALL NOT
blank the page on a single widget failure.

#### Scenario: Widget has no data

- **WHEN** a widget's dynamic payload is empty
- **THEN** the widget renders its empty state

#### Scenario: Widget-local dynamic error

- **WHEN** the dashboard payload reports a widget-local error in `dynamic.errors`
- **THEN** the error renders inside that widget and the rest of the dashboard stays usable
