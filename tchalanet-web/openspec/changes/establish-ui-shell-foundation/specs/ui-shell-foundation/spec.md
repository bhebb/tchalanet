# UI Shell Foundation

## ADDED Requirements

### Requirement: PageModel and shell responsibilities are separate

The PageModel renderer SHALL render only resolved content rows, columns, widgets, and widget
dynamic payloads. Public and private shells SHALL render page chrome from the resolved shell
contract.

#### Scenario: Render a public runtime page

- **WHEN** Angular receives a public runtime response
- **THEN** `PublicShell` renders `header`, content, and `footer`
- **AND** PageModel renders only the response content

#### Scenario: Render a private runtime page

- **WHEN** Angular receives a private runtime response
- **THEN** `PrivateShell` renders `topAppBar`, `navigationDrawer`, and routed content

### Requirement: Runtime navigation uses ActionItem

Reusable brand, navigation, overlay navigation, sidebar navigation, and footer components SHALL
consume `ActionItem` and shared destination helpers.

#### Scenario: Render internal and external actions

- **WHEN** an action destination kind is `route`
- **THEN** the component renders Angular route navigation
- **WHEN** an action destination kind is `url`
- **THEN** an external-capable component renders an external link

### Requirement: UI libraries have distinct responsibilities

`ui/theme` SHALL own runtime theme application, `ui/styles` SHALL contain compile-time SCSS
primitives, and `ui/components` SHALL contain reusable token-consuming components.

#### Scenario: Apply a runtime theme

- **WHEN** `ThemeStore` selects a runtime theme
- **THEN** `ThemeDomApplier` applies its tokens and synchronizes the root theme and overlay container
- **AND** components consume `--tch-*` tokens without mutating theme state

### Requirement: Frontend runtime is resolved

The Angular runtime SHALL NOT resolve storage bindings or JSON file keys.

#### Scenario: Render resolved runtime data

- **WHEN** the frontend receives a page runtime response
- **THEN** shell and widget components render the resolved camelCase payload directly
- **AND** no frontend file-key resolution request is made
