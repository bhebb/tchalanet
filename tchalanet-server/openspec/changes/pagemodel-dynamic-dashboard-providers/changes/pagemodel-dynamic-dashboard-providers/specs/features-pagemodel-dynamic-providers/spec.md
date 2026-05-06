# Spec — features.pagemodel dynamic providers

## ADDED Requirements

### Requirement: Dynamic providers resolve widgets by stable snake_case sources

The PageModel dynamic resolver SHALL resolve widgets whose binding mode is `dynamic` by matching `binding.source` against registered `PageModelDynamicProvider` implementations.

#### Scenario: Dynamic widget source has provider

- **GIVEN** a PageModel widget with `binding.mode = dynamic`
- **AND** `binding.source = cashier_recent_tickets`
- **WHEN** the PageModel is resolved
- **THEN** the resolver SHALL call the provider whose `supports(...)` matches `cashier_recent_tickets`
- **AND** the returned payload SHALL be placed under the widget id in `PageDynamicPayload.widgets`

#### Scenario: Dynamic widget source has no provider

- **GIVEN** a PageModel widget with `binding.mode = dynamic`
- **AND** no provider supports the configured source
- **WHEN** the PageModel is resolved
- **THEN** the resolver SHALL add a `WidgetDynamicError` with code `NO_PROVIDER`
- **AND** resolution of other widgets SHALL continue

### Requirement: JSON fragments are loaded only through a whitelisted registry

The system SHALL provide a generic `json_file` dynamic provider for reusable JSON fragments such as header links, footer links, sidebars, menus, support links, and legal links.

#### Scenario: Known JSON fragment key is loaded

- **GIVEN** a widget or shell section configured with `binding.source = json_file`
- **AND** props contain `file_key = private_sidebar_cashier`
- **WHEN** the provider loads the payload
- **THEN** it SHALL resolve `private_sidebar_cashier` through `PageModelJsonFragmentRegistry`
- **AND** load the mapped classpath resource
- **AND** parse it using the existing non-deprecated `JsonUtils` from `common`
- **AND** return the parsed JSON payload

#### Scenario: Raw path is attempted

- **GIVEN** a widget configured with `binding.source = json_file`
- **AND** props contain `file_key = ../../application.yml`
- **WHEN** the provider resolves the key
- **THEN** it SHALL reject the key because it is not present in the whitelist registry
- **AND** it SHALL NOT access the filesystem path

#### Scenario: Missing file key

- **GIVEN** a widget configured with `binding.source = json_file`
- **AND** props do not contain `file_key`
- **WHEN** the provider loads the payload
- **THEN** the resolver SHALL report a dynamic widget error with code `MISSING_PROP` or `PROVIDER_ERROR` with safe message

### Requirement: Providers do not own business rules

Dynamic providers in `features.pagemodel` SHALL compose UI payloads by calling QueryBus or stable application services, but SHALL NOT access persistence repositories, JPA entities, or implement business invariants.

#### Scenario: Cashier recent tickets provider loads data

- **GIVEN** a cashier dashboard PageModel
- **AND** a widget with `binding.source = cashier_recent_tickets`
- **WHEN** the provider loads the widget
- **THEN** it SHALL call a sales query such as `ListRecentTicketsQuery`
- **AND** it SHALL map the result to the widget payload
- **AND** it SHALL NOT query `ticket` tables directly

### Requirement: Public draw results provider supports home and details page

The public draw results provider SHALL support both a lightweight home widget and a fuller public results page through widget props.

#### Scenario: Public home requests no history

- **GIVEN** `binding.source = public_draw_results`
- **AND** props contain `include_history = false`
- **WHEN** the provider loads the payload
- **THEN** it SHALL return latest result by slot and next draw by slot
- **AND** it SHALL NOT include historical result rows

#### Scenario: Public results page requests history

- **GIVEN** `binding.source = public_draw_results`
- **AND** props contain `include_history = true`
- **AND** `history_limit = 10`
- **WHEN** the provider loads the payload
- **THEN** it SHALL return latest result by slot, next draw by slot, and up to 10 historical results per configured behavior

### Requirement: Dynamic output order is stable

The resolver SHOULD use an insertion-ordered map for dynamic payload output.

#### Scenario: PageModel widgets are resolved in document order

- **GIVEN** a PageModel with multiple dynamic widgets
- **WHEN** the resolver returns `PageDynamicPayload`
- **THEN** the dynamic widget map SHOULD preserve resolution order for debugging and snapshot tests
