# admin-maryaj-gratis-resilience Delta

## ADDED Requirements

### Requirement: Maryaj Gratis page separates game, offer, and generation concerns

The Maryaj Gratis admin page SHALL present game configuration, free-offer campaign configuration,
and generation behavior as separate business sections.

#### Scenario: Tenant admin reviews Maryaj Gratis setup

- **GIVEN** Maryaj Gratis game and campaign data are loaded
- **WHEN** the admin opens the Maryaj Gratis page
- **THEN** the page shows a game section
- **AND** an offer section
- **AND** a generation section
- **AND** no oversized duplicate summary card is required to understand the setup.

#### Scenario: Tenant game is missing or unavailable

- **GIVEN** campaign data loads but the Maryaj Gratis tenant game is missing or cannot be loaded
- **WHEN** the page renders
- **THEN** the page remains usable for campaign review where possible
- **AND** the game section shows a localized missing/degraded message
- **AND** the failure is not duplicated as shell feedback.

#### Scenario: Campaign is not instantiated

- **GIVEN** the Maryaj Gratis campaign does not exist for the tenant
- **WHEN** the page renders
- **THEN** the offer section shows a localized setup/activation state
- **AND** the page does not render as an empty blank page.

### Requirement: Permanent offer dates are business-facing

The Maryaj Gratis offer UI SHALL represent permanent offers with business copy instead of exposing
the backend's technical far-future date convention.

#### Scenario: Permanent campaign is displayed

- **GIVEN** the backend returns a Maryaj Gratis campaign with a far-future end date representing no
  end date
- **WHEN** the admin views the offer summary
- **THEN** the end state is displayed as `Pa gen fen` or the selected locale equivalent
- **AND** the technical stored date is not shown in the normal summary.

#### Scenario: Admin saves a permanent offer

- **GIVEN** the admin selects no end date for the offer
- **WHEN** the offer is saved
- **THEN** the web store maps the choice to the backend-compatible `endsAt` representation
- **AND** the admin does not need to enter or understand a far-future date.

#### Scenario: Admin selects an explicit end date

- **GIVEN** the admin chooses an explicit end date
- **WHEN** the offer is saved
- **THEN** the web store sends an explicit non-permanent end date
- **AND** it does not silently convert that end date into a permanent offer.

### Requirement: Offer rules are readable before editable

Maryaj Gratis tier and attribution rules SHALL be readable as business rules before the admin edits
the raw form fields.

#### Scenario: Tiered offer is displayed

- **GIVEN** the campaign effect uses tiered paid amounts
- **WHEN** the offer summary renders
- **THEN** each tier is shown as a range-to-free-Maryaj rule
- **AND** open-ended final tiers are shown as a business-facing open range.

#### Scenario: Tier configuration is invalid

- **GIVEN** the admin edits tier ranges or quantities
- **WHEN** the form validation runs
- **THEN** invalid min/max, overlaps, missing tiers, non-positive quantities, and misplaced
  open-ended ranges show localized validation feedback
- **AND** frontend validation does not reimplement backend payout or eligibility logic.

### Requirement: Maryaj Gratis game editing is sectioned by tenant-admin tasks

The Maryaj Gratis game editing surface SHALL separate selling controls, stake limits, draw
availability, game options, payout rules, and advanced settings instead of rendering one long
technical form.

#### Scenario: Tenant admin edits selling controls

- **GIVEN** the admin opens Maryaj Gratis game editing
- **WHEN** the game editor renders
- **THEN** game activation and POS visibility are presented in a `Vant` section
- **AND** the labels distinguish tenant enablement from POS visibility.

#### Scenario: Tenant admin edits stake limits

- **GIVEN** the admin opens Maryaj Gratis game editing
- **WHEN** the stake section renders
- **THEN** minimum and maximum stakes are grouped together
- **AND** amounts are displayed with HTG formatting
- **AND** validation feedback appears next to the invalid field.

#### Scenario: Tenant admin reviews draw availability

- **GIVEN** the admin opens Maryaj Gratis game editing
- **WHEN** the availability section renders
- **THEN** the editor provides a clear action to `Disponibilite pa tiraj`
- **AND** it does not embed the full game-by-draw matrix inside the Maryaj Gratis editor.

#### Scenario: Tenant admin reviews game options and payout rules

- **GIVEN** Exact and Reverse options are supported by the catalog
- **WHEN** the game editor renders
- **THEN** supported options are shown as compact editable rows
- **AND** payout rules are shown as compact summaries before individual rule editing
- **AND** payout business logic remains owned by the existing backend/API contracts.

### Requirement: Maryaj Gratis offer editing separates lifecycle, attribution, tiers, and advanced settings

The Maryaj Gratis offer editor SHALL expose campaign dates/status, attribution mode, tier rules, and
advanced settings as separate concerns.

#### Scenario: Tenant admin configures offer dates

- **GIVEN** the admin edits the Maryaj Gratis offer
- **WHEN** the lifecycle section renders
- **THEN** the admin can choose no end date without entering a technical far-future date
- **AND** choosing a real end date reveals the date control.

#### Scenario: Tenant admin configures attribution

- **GIVEN** the admin selects an attribution mode
- **WHEN** the selected mode uses tier rules
- **THEN** the tier editor is shown
- **AND** irrelevant tier controls are hidden when the selected mode does not use tiers.

#### Scenario: Tenant admin edits tiers

- **GIVEN** multiple tier rules exist
- **WHEN** the admin opens tier editing
- **THEN** the rules are readable as business summaries first
- **AND** at most one tier is expanded for editing at a time
- **AND** open-ended final tiers use an explicit business-facing control or state.

#### Scenario: Tenant admin opens advanced offer settings

- **GIVEN** priority or other rarely used settings are available
- **WHEN** the offer editor renders
- **THEN** those fields are secondary to the normal campaign flow
- **AND** hidden advanced values are preserved on save.

### Requirement: Maryaj Gratis advanced settings are secondary

Settings that are rarely needed by tenant admins SHALL be visually secondary to normal game and
offer setup.

#### Scenario: Admin reviews normal offer settings

- **GIVEN** the offer uses the default priority
- **WHEN** the admin opens the offer editor
- **THEN** priority is available only in an advanced section
- **AND** the primary offer summary does not require understanding the numeric priority value.

#### Scenario: Admin reviews generation behavior

- **GIVEN** Maryaj Gratis generation parameters are loaded
- **WHEN** the page renders
- **THEN** selection mode and retry count are shown in a generation section
- **AND** generation behavior is not duplicated inside the game section.

### Requirement: Maryaj Gratis uses web console, i18n, theme, and error conventions

The Maryaj Gratis page SHALL follow the private-console conventions for shell structure, i18n,
styling, theming, and error ownership.

#### Scenario: Page renders in any supported locale

- **GIVEN** the locale is HT, FR, or EN
- **WHEN** the page and edit surfaces render
- **THEN** user-visible labels, helper text, statuses, validation, dialogs, empty states, and errors
  come from i18n bundles
- **AND** raw translation keys are not shown as user copy.

#### Scenario: A blocking load failure occurs

- **GIVEN** the campaign load fails
- **WHEN** the page renders
- **THEN** the page shows one blocking page-level error with retry
- **AND** section or shell feedback does not duplicate the same failure.

#### Scenario: A section-level degradation occurs

- **GIVEN** optional game configuration data fails while campaign data is available
- **WHEN** the page renders
- **THEN** the page keeps the offer/generation content usable
- **AND** the degraded section uses localized section-level feedback.

#### Scenario: Maryaj Gratis styling is reviewed

- **GIVEN** local component styles are inspected
- **WHEN** colors, spacing, radius, and responsive layout are defined
- **THEN** they consume existing `--tch-*` theme tokens or component-local styles
- **AND** the feature does not introduce new global theme tokens or cross-feature CSS dependencies.

#### Scenario: Maryaj Gratis mobile surfaces are verified

- **GIVEN** the page or editor is rendered at approximately 360 px width
- **WHEN** the admin reviews or edits Maryaj Gratis configuration
- **THEN** content avoids nested card-heavy layouts
- **AND** touch targets remain usable
- **AND** sticky save actions do not obscure the final controls
- **AND** numeric keyboards do not hide required validation or save/cancel controls.

#### Scenario: Maryaj Gratis tests are reviewed

- **GIVEN** the Maryaj Gratis UX refactor is ready for review
- **WHEN** focused validation runs
- **THEN** unit coverage includes permanent offers, tier validation, editor save/cancel behavior, and API mapping
- **AND** e2e coverage includes main-page states, game editor paths, offer editor paths, i18n leakage, and responsive smoke checks.
