# public-design-system-pagemodel-widgets

## ADDED Requirements

### Requirement: Public pages use semantic design tokens

Public Angular components SHALL consume semantic design tokens for colors, surfaces, borders, typography, spacing, and status colors.
Components SHALL NOT hardcode brand colors as one-off component styling.
The existing base `tchalanet` theme preset SHALL be updated to the public Deep Blue + Gold direction rather than creating a parallel public-only theme.
The public theme SHALL remain based on Material Design 3 generated tonal palettes and SHALL support light, dark, and system modes from the first implementation.

#### Scenario: Public component renders a primary action

- **WHEN** a public widget renders a critical primary action
- **THEN** it uses the semantic action tokens mapped to `secondaryContainer` and `onSecondaryContainer`

#### Scenario: Theme payload is incomplete

- **WHEN** a PageModel or theme payload omits optional public theme tokens
- **THEN** the UI uses safe token fallbacks and remains readable

#### Scenario: Base preset is generated

- **WHEN** the `tchalanet` preset is generated
- **THEN** it uses `#1A1B4B` as the official primary base
- **AND** it exposes Gold/Yellow action semantics through `secondaryContainer` and `--tch-color-secondary-container`

#### Scenario: Dark mode is active

- **WHEN** the public surface renders with `.tch-theme.dark[data-preset='tchalanet']`
- **THEN** public widgets use the same semantic `--tch-*` token names
- **AND** the values are derived from Material Design 3 dark system tokens or dark-safe runtime mappings
- **AND** the page remains readable without component-local dark-mode color constants

### Requirement: Public Angular code follows page, container, facade, store, component architecture

Public Angular code SHALL follow the existing `Route -> Page -> Container(s) -> Component(s)` architecture.
Routes SHALL point to `*.page.ts` components.
Pages and containers MAY inject facades, stores, router, and runtime services.
Visual components and widgets SHALL be presentational and SHALL NOT inject API clients, facades, NgRx store, or feature stores.
Facades SHALL mediate page/container commands, store selectors, services, navigation, and PageModel loading.
Feature stores SHALL own screen/API state such as loading/error, filters, selected result, verification response, and rules/simulation payload state.

#### Scenario: Route is configured

- **WHEN** a public route is added
- **THEN** the route points to a `*.page.ts` component
- **AND** it does not route directly to a container, visual component, or widget

#### Scenario: Page loads API-backed public data

- **WHEN** a public page needs PageModel, verification, results, rules, or simulation data
- **THEN** the page or an internal container uses a facade/store boundary
- **AND** visual components receive the resulting typed inputs

#### Scenario: Widget renders PageModel props

- **WHEN** a public widget receives typed props, explicit state, dynamic payload, and typed action descriptors
- **THEN** it renders from those inputs only
- **AND** it emits typed user intent rather than navigating or calling APIs directly

#### Scenario: Widget is reused on another public page

- **WHEN** the same widget is placed on another public PageModel page with compatible props
- **THEN** it renders without depending on page-specific services, route params, private navigation, or hardcoded copy

### Requirement: Public Angular files use inline templates/styles for this slice

New public Angular pages, containers, components, and widgets in this slice SHALL use inline `template` and inline `styles` unless an existing local pattern or maintainability issue requires extraction.

#### Scenario: New public widget is added

- **WHEN** a new public widget file is created for this slice
- **THEN** its template and styles are inline in the TypeScript file
- **AND** it continues to consume reusable `--tch-*` variables

### Requirement: Public CSS is scoped, tokenized, and theme-safe

Public page and widget CSS SHALL use scoped semantic class names and reusable `--tch-*` variables.
Public CSS SHALL NOT hardcode brand hex values, status hex values, Material theme role values, or one-off component colors.
Public page text SHALL use i18n keys or localized PageModel labels even when copy is temporary.

#### Scenario: Public component defines classes

- **WHEN** a public page, widget, or component adds CSS classes
- **THEN** class names use a scoped BEM-like convention such as `block`, `block__element`, `block--modifier`, and `is-state`
- **AND** generic unscoped class names such as `card`, `button`, `title`, `section`, `container`, or `active` are avoided

#### Scenario: Public component uses theme values

- **WHEN** public CSS needs a color, surface, border, focus ring, typography, spacing, or radius
- **THEN** it uses semantic `--tch-*` variables with Material system fallbacks where useful
- **AND** it does not hardcode values such as `#1A1B4B`, `#2E3192`, `#FECB00`, or status hex values in component styles

#### Scenario: Public page is validated across themes

- **WHEN** a new or materially changed public page is completed
- **THEN** it is browser-checked at mobile and desktop breakpoints
- **AND** it is checked in light and dark mode
- **AND** it is checked against the base `tchalanet` Material theme plus one alternate theme when an alternate preset is available locally

### Requirement: Specs are focused on logic

The implementation SHALL add specs for facades, stores, containers, and widgets/components with meaningful logic.
Purely visual stateless components that only render inputs SHALL NOT require low-value unit specs.

#### Scenario: Component contains branching or validation logic

- **WHEN** a widget/component maps states, emits outputs, validates form input, handles i18n fallback, or guards unsafe copy
- **THEN** it has a focused spec for that logic

#### Scenario: Component is visual only

- **WHEN** a stateless component only renders provided inputs with tokenized styling
- **THEN** a unit spec is optional and not required by this change

### Requirement: Public copy is internationalized from the start

Public page copy SHALL use the existing i18n system from the first implementation.
Angular templates SHALL use translation keys or localized PageModel labels rather than hardcoded public prose.

#### Scenario: Public navigation renders in French

- **WHEN** the active language is French
- **THEN** public navigation labels resolve through `public.nav.*` i18n keys
- **AND** the rendered labels include `Résultats`, `Vérifier un ticket`, `Aide`, `Pour opérateurs`, and `Connexion`

#### Scenario: Translation is missing

- **WHEN** a public widget references a missing translation key
- **THEN** the existing stable fallback behavior keeps the widget visible
- **AND** the failure does not blank the public page

### Requirement: Public navigation is anonymous-safe

The public shell SHALL show only public navigation to anonymous users.
Public pages SHALL NOT show `Profile`, private dashboard links, admin navigation, cashier navigation, or tenant admin actions to anonymous users.

#### Scenario: Anonymous user opens public page on desktop

- **WHEN** an anonymous user opens a public route on desktop
- **THEN** the header shows `Tchalanet`, `Résultats`, `Vérifier un ticket`, `Aide`, `Pour opérateurs`, and `Connexion`
- **AND** it does not show private profile navigation

#### Scenario: Anonymous user opens public page on mobile

- **WHEN** an anonymous user opens a public route on mobile
- **THEN** the bottom navigation shows `Résultats`, `Vérifier`, and `Aide`
- **AND** it does not show private profile navigation

### Requirement: Public pages are composed from reusable PageModel widgets

Public pages SHALL be composed from reusable widgets with stable props and typed action destinations.
Each public widget SHALL support default, loading, empty, error, mobile, and desktop states.
Widgets SHALL style through reusable `--tch-*` tokens and SHALL be compatible with Material Design 3 light and dark mode.
Public PageModel widgets SHALL be treated as typed contracts and SHALL define `type`, `id`, `props`, optional `state`, and typed actions.
Shared widget state SHALL use `default`, `loading`, `empty`, `error`, or `partial`.
Domain-specific statuses SHALL be explicit typed enums.

#### Scenario: Widget contract is defined

- **WHEN** a public widget is added to the registry
- **THEN** its contract includes `type`, `id`, `props`, optional `state`, and typed actions
- **AND** the Angular component is only the renderer for that contract

#### Scenario: PageModel action is rendered

- **WHEN** a public widget receives an action prop
- **THEN** the action uses a typed destination: `{ type: 'path'; path: string }`, `{ type: 'external'; url: string }`, or `{ type: 'anchor'; id: string }`
- **AND** raw href strings are not used inside widget props unless explicitly mapped at the component boundary

#### Scenario: Widget data is temporarily unavailable

- **WHEN** a public widget has no usable data because it is loading, empty, or failed
- **THEN** the widget renders a contained state without blanking the whole public page

### Requirement: Ticket verification uses cautious public wording

The ticket verification UI SHALL describe verification as a public ticket status lookup.
It SHALL NOT use gambling, direct-payout, or winner language.
Ticket verification status SHALL use the typed values `PENDING_RESULT`, `NOT_PAYABLE`, `PAYABLE`, `INVALID_OR_CANCELLED`, `NOT_FOUND`, and `SERVICE_UNAVAILABLE`.

#### Scenario: User opens ticket verification

- **WHEN** the user opens `/public/check-ticket`
- **THEN** the page shows `Vérifier un ticket`
- **AND** the form asks for `Code public du ticket`
- **AND** the primary action says `Vérifier maintenant`

#### Scenario: Ticket is payable

- **WHEN** verification returns `PAYABLE`
- **THEN** the UI instructs the user to follow the displayed point-of-sale instructions and keep the original receipt
- **AND** the UI does not say that Tchalanet pays directly

#### Scenario: Verification service is unavailable

- **WHEN** verification returns `SERVICE_UNAVAILABLE`
- **THEN** the UI shows `Service temporairement indisponible`

### Requirement: Results pages present supported confirmed results without certification claims

The results UI SHALL present draw results using cautious source wording.
Result status SHALL use the typed values `CONFIRMED`, `PENDING`, and `UNAVAILABLE`.

#### Scenario: Results list renders confirmed result

- **WHEN** `/public/results` renders a confirmed draw result
- **THEN** the result card shows `Résultats confirmés`
- **AND** the surrounding copy may say `Résultats confirmés selon les sources prises en charge.`
- **AND** it does not claim official certification unless a future legal decision explicitly permits it

#### Scenario: Result detail renders

- **WHEN** `/public/results/:id` renders a result detail
- **THEN** the page shows game name, draw date/time, status, numbers, supported source, last update, and a `Vérifier un ticket` action

### Requirement: Rules and simulations are indicative only

Rules and simulation widgets SHALL present simulations as indicative and SHALL NOT hardcode odds, multipliers, or payout rules in frontend code.
Simulation status SHALL use the typed values `NO_GAME_SELECTED`, `GAME_SELECTED`, `RULES_UNAVAILABLE`, `INVALID_SELECTION`, `INVALID_STAKE`, `SIMULATION_UNAVAILABLE`, and `CALCULATED`.
The frontend SHALL NOT compute payouts, odds, multipliers, or game pricing from hardcoded values.
The simulation widget SHALL only display backend/API/PageModel payload values, validation errors, or unavailable states.

#### Scenario: User opens rules and simulation page

- **WHEN** the user opens `/public/rules`
- **THEN** the page shows `Règles des jeux et simulation`
- **AND** it includes both rules content and a `SimulationWidget`
- **AND** it does not require authentication

#### Scenario: User calculates a simulation

- **WHEN** a simulation returns a calculated estimate
- **THEN** the UI labels the value as `Gain estimé indicatif`
- **AND** it displays the required simulation disclaimer

#### Scenario: Rules data is unavailable

- **WHEN** rules or pricing data is unavailable
- **THEN** the UI renders a `RULES_UNAVAILABLE` or `SIMULATION_UNAVAILABLE` state rather than inventing local pricing values

#### Scenario: Simulation data is missing

- **WHEN** simulation data is missing for a selected game
- **THEN** the UI shows `La simulation est temporairement indisponible pour ce jeu.`
- **AND** it still displays the required simulation disclaimer

### Requirement: V1 scope prioritizes design system and first public pages

This change SHALL prioritize the public design system, anonymous-safe navigation, typed widget contracts, and first public pages.
The full rules/simulation implementation and news implementation SHALL NOT block V1 public design-system delivery.

#### Scenario: Rules or news backend payload is not ready

- **WHEN** full rules/simulation or news payloads are not available during V1
- **THEN** V1 may ship PageModel-compatible empty or unavailable states
- **AND** the full data-backed behavior remains follow-up scope

### Requirement: Public V1 routes exist

The web app SHALL provide the V1 public routes for home, ticket verification, results, result detail, rules/simulation, help, contact, privacy, and terms.

#### Scenario: Public V1 route is requested

- **WHEN** a user navigates to a V1 public route
- **THEN** the route renders a public PageModel-compatible surface without requiring authentication
