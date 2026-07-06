## ADDED Requirements

### Requirement: Stable lottery technical terms render through console display helpers

Admin and platform console surfaces SHALL render stable lottery technical codes through
`@tch/web/console` helpers or pipes instead of page-local label maps.

#### Scenario: A console surface displays a game code

- **GIVEN** a console surface displays a game code such as `HT_BOLET`, `HT_LOTO3`, `HT_LOTO4`, or
  `HT_LOTO5`
- **WHEN** the value is rendered in a table, detail page, ticket view, print view, pricing/barème
  view, or stats view
- **THEN** it uses `consoleGameName` or `ConsoleGameNamePipe`
- **AND** unknown codes fall back to a readable label instead of rendering blank text.

#### Scenario: A game page displays a known game

- **GIVEN** a game page, game card, game setup view, ticket detail, print line, or pricing/barème
  row displays a known game
- **WHEN** a readable game name exists
- **THEN** the visible game identity uses the readable game name such as `Bòlèt`, `Maryaj`, or
  `Loto 3`
- **AND** the technical code such as `HT_BOLET` is not shown in the normal user-facing game label.

#### Scenario: A game matrix displays a known game

- **GIVEN** the game matrix displays a known game such as Bòlèt, Maryaj, Maryaj gratis, Loto 3,
  Loto 4, or Loto 5
- **WHEN** a dedicated game logo asset exists
- **THEN** the game identity displays the logo and readable game label
- **AND** compact text such as `Bo`, `Ma`, or `L3` is used only as a fallback when the logo is
  unavailable.

#### Scenario: A French UI displays a Lotto technical family

- **GIVEN** a backend enum, bet type, route id, data id, or i18n key contains `LOTTO` or `lotto`
- **WHEN** the value is displayed as French user-facing text
- **THEN** the visible label uses `Loto`, such as `Loto 3`, `Loto 4`, or `Loto 5`
- **AND** `Lotto` is reserved for an official external provider/product name, not Tchalanet's
  French game label.

#### Scenario: A console surface displays a bet type and option

- **GIVEN** a console surface displays a bet type and optional bet option
- **WHEN** it renders ticket lines, print lines, pricing/barèmes, limits, stats, or game setup
- **THEN** it uses `consoleBetLabel`, `consoleBetTypeLabel`, or `consoleBetOptionLabel`
- **AND** it does not define a local mapping for the same bet type or option code.

### Requirement: Bet options are defined by a domain glossary

Bet type and bet option display SHALL be defined by a structured domain glossary, not directly by
Angular pipes or page-local mappings.

#### Scenario: A known bet option is rendered

- **GIVEN** a bet type and option pair such as `LOTTO4_PATTERN` option `3`
- **WHEN** any console, ticket, print, pricing/barème, or stats surface formats the value
- **THEN** the label comes from the domain bet glossary
- **AND** both pure helper functions and pipes return the same label.

#### Scenario: A sellable bet option list is displayed

- **GIVEN** a game setup, pricing/barème, ticket entry, ticket detail, print, or stats surface needs
  bet option labels
- **WHEN** it builds the options for a sellable flow
- **THEN** it uses the supported bet option helper/glossary
- **AND** it only displays options supported by backend sale validation and winning calculation.

#### Scenario: Provider documentation lists extra combinations

- **GIVEN** a provider page lists combinations such as `3-way box`, `6-way box`, `12-way box`,
  `24-way box`, or `straight/box`
- **WHEN** the backend support matrix does not define those combinations as explicit bet options
- **THEN** web does not show them as sellable options
- **AND** documentation/debug displays mark them unsupported or deferred.

#### Scenario: A result page displays combinations and rules

- **GIVEN** a result detail page has a current draw result with actual winning numbers and derived
  Haiti/provider result facts
- **WHEN** the page renders `Combinaisons & règles`
- **THEN** the rows are derived from the current result facts or backend explanation response
- **AND** web does not render a static provider documentation matrix as if it belonged to the
  selected result.

#### Scenario: A new bet pattern is introduced

- **GIVEN** a backend-compatible bet type, bet option, or pattern code is introduced
- **WHEN** web needs to display it
- **THEN** the glossary is updated with the game family, technical code, option number when present,
  short label, and long meaning
- **AND** no component-local switch/table is added for that same term.

#### Scenario: A bet option is unknown

- **GIVEN** a bet type is known but the option number is not in the glossary
- **WHEN** the value is displayed
- **THEN** the fallback is deterministic, such as `Option N`
- **AND** missing labels never render as an empty string when a source value exists.

### Requirement: Draw and result identity uses one provider/channel/display contract

Admin and platform console surfaces SHALL share one display contract for provider logos, slot logos,
draw channels, result-slot labels, and Haiti lot mappings.

#### Scenario: A draw channel has a legacy composed name

- **GIVEN** a draw channel has a stable code such as `HT_NY_MID` and a backend name such as
  `Haïti • New York • Midday`
- **WHEN** a console surface renders the draw channel identity
- **THEN** the display helper derives the preferred label from stable code, provider, period/time,
  and result-slot data when available
- **AND** the composed backend `name` is treated as fallback or tenant override, not as the canonical
  technical identity.

#### Scenario: A draw or draw result needs a provider or slot logo

- **GIVEN** a draw, draw result, result slot, or draw channel has a provider/channel/slot code
- **WHEN** a console page needs a logo or image URL
- **THEN** it uses a `@tch/web/console` lottery asset helper
- **AND** the underlying image files remain owned by `@tch/shared-assets`.

#### Scenario: A draw result entry needs lot labels

- **GIVEN** a result entry displays Haiti lot rows such as first, second, and third lot
- **WHEN** admin/platform dialogs or detail pages render the rows
- **THEN** they use the shared Haiti lot/game mapping helper from `@tch/web/console`
- **AND** admin and platform do not keep duplicated mapping tables.

#### Scenario: An operator enters a manual or override result

- **GIVEN** an operator opens a manual result or override result dialog for a provider slot
- **WHEN** the dialog asks for `lot1`, `lot2`, and `lot3`
- **THEN** it displays the Haiti lot mapping before the inputs
- **AND** each lot card shows the Haiti lot label, provider game label, provider identity, and
  provider game logo.

#### Scenario: A tenant admin completes an incomplete draw result manually

- **GIVEN** a draw result is missing or incomplete after the configured manual-entry delay
- **WHEN** a tenant admin has the manual result-entry capability
- **THEN** the admin can enter the missing result values manually
- **AND** the entry is treated as completion of an incomplete result, not as an override of an
  existing confirmed result.

#### Scenario: A protected result action is displayed

- **GIVEN** a draw result already exists or is provisional
- **WHEN** the UI offers confirm or override actions
- **THEN** those actions are restricted to super admin or explicit platform operations capability
- **AND** tenant admin completion rights do not imply confirm or override rights.

#### Scenario: A provider has a known Pick 3 and Pick 4 product

- **GIVEN** the result slot belongs to a known provider such as `NY`, `TX`, `FL`, or `GA`
- **WHEN** web builds the Haiti lot provider mapping
- **THEN** `lot1` maps to the provider Pick 3 product label/logo
- **AND** `lot2` and `lot3` map to the provider Pick 4 product label/logo
- **AND** the provider-specific product names are used, such as `Numbers` and `Win 4` for `NY`.

#### Scenario: A provider does not have an explicit product mapping

- **GIVEN** the result slot provider is unknown or does not have an explicit Pick 3/Pick 4 mapping
- **WHEN** the manual/override result dialog displays the Haiti lot mapping
- **THEN** it uses deterministic fallback labels `Pick 3` and `Pick 4`
- **AND** it uses fallback Pick 3/Pick 4 logos
- **AND** it does not render a missing image or blank label.

#### Scenario: A result slot is displayed

- **GIVEN** a result slot has `slotKey`, optional provider, optional draw time, and optional label
- **WHEN** it is rendered in draw results, operations, or details
- **THEN** it uses the shared result-slot label helper
- **AND** the same input produces the same label in admin and platform.

#### Scenario: A draw identity needs short and long labels

- **GIVEN** a draw, draw result, or draw channel has provider, slot, channel, date, and time
  metadata
- **WHEN** a web surface builds its view model
- **THEN** it builds a structured draw identity containing provider short label, provider long
  label, channel short label, channel long label, slot labels, logo URL, draw date, official/provider
  date/time, local/tenant date/time, timezone labels, and stable code fallback
- **AND** page components do not independently re-derive provider names, slot period names, or logo
  URLs from raw strings.

#### Scenario: Draw labels are read from typed identities

- **GIVEN** web needs to display a draw, draw channel, or result slot label
- **WHEN** raw API data is mapped into a view model
- **THEN** it first builds a typed identity with `consoleDrawIdentity`,
  `consoleDrawChannelIdentity`, or `consoleResultSlotIdentity`
- **AND** components read explicit fields such as `channelShortLabel`, `channelLongLabel`,
  `providerShortLabel`, `providerLongLabel`, and `providerLogoUrl`
- **AND** broad string formatter helpers that accept many unrelated input shapes are not introduced.

#### Scenario: An admin result displays local date and time

- **GIVEN** an admin draw-result row has an official/provider timestamp and a tenant/local timezone
- **WHEN** the result is rendered in the admin results table or detail view
- **THEN** the view model exposes the official/provider date and time separately from the
  tenant/local date and time
- **AND** the local date is shown when it differs from the official/provider date
- **AND** fetched/applied timestamps are not used as substitutes for the draw's local date/time.

#### Scenario: A public result surface displays draw identity

- **GIVEN** the public latest-results list or public result-detail page renders a result
- **WHEN** it displays provider, channel, slot, draw date, draw time, receipt text, or related
  result labels
- **THEN** the labels follow the same draw identity semantics as admin/platform
- **AND** public-specific copy and PageModel/i18n ownership remain public-owned.

### Requirement: Draw identity is responsive by density, not by redefining labels per page

Public, admin, and platform surfaces SHALL choose from the same draw identity fields for mobile,
tablet, desktop, and print/receipt layouts.

#### Scenario: Draw identity renders on mobile

- **GIVEN** a mobile card displays a draw, draw result, or draw channel
- **WHEN** horizontal space is constrained
- **THEN** it prioritizes provider logo or short provider label, compact slot/period label, draw
  date, and official/provider time
- **AND** admin/operator cards may show local date/time as a secondary compact line
- **AND** long composed channel names are not used as the primary first-line label.

#### Scenario: Draw identity renders on tablet

- **GIVEN** a tablet layout displays a draw, draw result, or draw channel
- **WHEN** the surface uses compact rows or a two-column card grid
- **THEN** it may use long provider/channel labels where space allows
- **AND** it keeps official/provider date/time and local/tenant date/time distinct from fetched and
  applied audit timestamps.

#### Scenario: Draw identity renders on desktop

- **GIVEN** a desktop table displays draw identity
- **WHEN** there is enough column space
- **THEN** it may display the long provider/channel label such as `New York · Midi`, a secondary
  stable code, separate official/provider date/time, and separate local/tenant date/time
- **AND** it does not fall back to app-local provider maps when the shared identity helper can
  resolve the same values.

#### Scenario: Draw identity renders on print or receipt surfaces

- **GIVEN** a ticket print, receipt-like result detail, or printable summary displays a draw
- **WHEN** responsive abbreviation is not required
- **THEN** it uses the long provider/channel label, stable game/bet labels, draw date, official
  time, local/tenant date/time when relevant, and short code only as reference
- **AND** it does not introduce print-only label mappings.

### Requirement: Console actor identity uses one display contract

Platform console surfaces SHALL render super-admins, tenant-admins, seller terminals, and seller
actors through a shared `@tch/web/console` actor identity model instead of app-local user-card
models per role.

#### Scenario: A platform page displays a super-admin

- **GIVEN** a platform page loads a super-admin from the platform-scoped super-admin API
- **WHEN** the page renders the user card, list row, or recipient option
- **THEN** the API DTO is adapted into `ConsoleActorIdentity`
- **AND** the visible identity uses the shared console actor component/model
- **AND** the component does not know the actor came from `/platform/super-admins`.

#### Scenario: A tenant page displays a tenant-admin

- **GIVEN** a platform page loads a tenant-admin through tenant-scoped identity access
- **WHEN** the page renders the user card, list row, or recipient option
- **THEN** the API DTO is adapted into the same `ConsoleActorIdentity`
- **AND** tenant scope fields such as tenant id/code/name are carried on the identity
- **AND** the tenant-scoped data-access layer remains responsible for `asTenantAdmin` options.

#### Scenario: A recipient picker displays mixed actors

- **GIVEN** a recipient picker returns super-admins, tenant-admins, and seller terminals
- **WHEN** it displays search options
- **THEN** each option is derived from the shared actor identity fields
- **AND** local per-role label/status formatting is not duplicated in the picker.

#### Scenario: A seller terminal is displayed as an actor

- **GIVEN** a seller terminal has an id, code/name, status, and tenant scope
- **WHEN** it is displayed in support, recipient, detail, or audit surfaces
- **THEN** it uses the same actor identity contract with actor kind `SELLER_TERMINAL`
- **AND** missing person fields such as email do not produce blank labels.

### Requirement: Display helpers support print, tickets, details, and stats

The console display vocabulary SHALL be reusable outside interactive tables, including print and
summary surfaces.

#### Scenario: A ticket print or ticket detail view renders a line

- **GIVEN** a ticket line includes game, bet type, bet option, draw, and provider/channel metadata
- **WHEN** the ticket is displayed or printed
- **THEN** it uses the same `@tch/web/console` display helpers as console tables
- **AND** print-specific formatting does not introduce separate label mappings.

#### Scenario: A stats/report surface groups by technical code

- **GIVEN** a report or stats view groups by game, bet type, draw channel, provider, or draw status
- **WHEN** it renders group labels
- **THEN** it uses the canonical display helpers
- **AND** totals and exported/printable summaries use the same labels as on-screen tables.

### Requirement: Pipes are template adapters over pure helpers

Angular pipes for technical labels SHALL be thin wrappers over pure helper functions in
`@tch/web/console`.

#### Scenario: A new technical label pipe is added

- **GIVEN** repeated template usage justifies a pipe
- **WHEN** a new pipe is added for a technical term
- **THEN** the formatting logic lives in a pure exported helper function
- **AND** the pipe delegates to that helper.

#### Scenario: TypeScript code formats a technical label

- **GIVEN** a service, view-model mapper, print formatter, or report mapper needs a technical label
- **WHEN** it formats the value
- **THEN** it uses the pure helper function directly
- **AND** it does not instantiate or depend on an Angular pipe.
