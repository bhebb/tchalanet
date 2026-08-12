# Tasks

## 0. Discovery

- [x] Confirm existing setup page separates required and optional cards.
- [x] Confirm backend readiness keeps `limits`, `commission`, and `subscription` out of readiness rollup.
- [x] Confirm games, draw channels, tenant settings, and seller terminals remain separate ownership surfaces.

## 1. Setup Page: Readiness vs Operational Setup

- [ ] Keep existing blocking/readiness semantics unchanged.
- [ ] Keep required cards focused only on configuration required to sell.
- [ ] Review readiness labels and replace technical wording with business wording.
- [ ] Ensure the primary setup summary clearly answers ready to sell, not ready to sell, and number of blocking items.
- [ ] Rename/reframe the optional area as operational setup or equivalent business wording.
- [ ] Visually separate operational cards from required readiness cards.
- [ ] Add POS & Printing as an operational setup card.
- [ ] Show a simple operational status: Configured, Not configured, Recommended, or Not enabled.
- [ ] Link POS & Printing to the existing tenant settings print configuration.
- [ ] Keep limits, commission, subscription, notifications, Maryaj Gratis, and similar non-blocking concerns in the operational section.
- [ ] Ensure every setup problem exposes one primary corrective action.
- [ ] Avoid multiple competing CTAs for the same problem.
- [ ] Cover setup card ordering, optional/operational behavior, readiness summary, and corrective destinations with focused tests.

## 2. Games Configuration UX

- [ ] Redesign games overview cards around business configuration instead of implementation concepts.
- [ ] Show a clear overall sale configuration status: Ready, Needs attention, or Disabled.
- [ ] Separate game cards into activation, POS visibility, stake limits, pricing/payout, and advanced options.
- [ ] Add a first-class available-on-draws summary.
- [ ] Show the number of draws/channels where the game can currently be sold.
- [ ] Add a clear Review availability action.
- [ ] Restructure the game settings dialog into clear sections: activation, POS visibility, stake limits, pricing/payout, advanced options.
- [ ] Add payout/stake preview copy where current API data is reliable.
- [ ] Keep Maryaj Gratis as a visible linked action, not a duplicated inline form.
- [ ] Show Maryaj Gratis configuration status and route its primary action to the dedicated Maryaj Gratis page.
- [ ] Prefer business labels such as Available on, Visible on POS, Stake limits, Pricing / payout, and Needs attention.
- [ ] Avoid exposing internal mapping/entity terminology.
- [ ] Ensure all labels are i18n keys and raw game codes are secondary/fallback only.

## 3. Draw Channels UX: Sale Availability

- [ ] Treat draw channels as a tenant-facing aggregated configuration surface.
- [ ] Preserve backend ownership boundaries; compose existing public APIs/read models where required.
- [ ] Model primary sale status separately from result-source mode.
- [ ] Use primary sale statuses: Ready, Needs attention, Disabled.
- [ ] Use secondary attributes: Automatic/Manual, number of available games, number of upcoming/generated draws.
- [ ] Redesign draw-channel cards using business labels: result source, automatic results, manual results, draw time, sales close, available games, upcoming draws.
- [ ] Add next actions for Configure game availability and Review schedule.
- [ ] Detect and surface configured channels with no useful sellable coverage.
- [ ] Distinguish no upcoming/generated draws from upcoming draws with no games available.
- [ ] Ensure no upcoming draws routes to schedule review.
- [ ] Ensure no games available routes to game availability/draw configuration.
- [ ] Ensure manual mode does not imply incomplete and automatic mode does not imply ready.
- [ ] Surface incomplete states without raw technical codes or overloaded status.
- [ ] Keep provider/channel names from backend display data; fallback to stable code only when no display label exists.

## 4. Tenant Settings: Global Defaults

- [ ] Keep tenant settings as owner of tenant-wide defaults.
- [ ] Keep currency, locale/calendar, receipt defaults, PDF defaults, POS/printing defaults, delivery channels, branding, and address in tenant settings.
- [ ] Improve setup links so admins land directly in the relevant tenant settings section where possible.
- [ ] Do not duplicate tenant settings forms inside setup.

## 5. Seller Terminals: Defaults vs Overrides

- [ ] Keep terminal-specific configuration on seller terminal management.
- [ ] Preserve Sunmi integrated printer, generic ESC/POS, PDF, 58 mm, 80 mm, A4, auto-print, Bluetooth printer, and test print options.
- [ ] Clearly show whether a printer/POS value comes from Tenant default or Terminal override.
- [ ] Keep test-print action terminal-specific.
- [ ] Add contextual links from tenant settings print/POS configuration back to seller terminal overrides where useful.
- [ ] Add contextual links from seller terminal details back to tenant default receipt/POS configuration.

## 6. Shared UX Rules

- [ ] Define a deterministic corrective destination for every configuration warning.
- [ ] Do not send admins to a generic settings page if a specific destination exists.
- [ ] Avoid duplicating the same setting across several pages.
- [ ] Hide implementation terms such as provider client, source config, result slot, tenant-game mapping, generated entity, and BFF from primary UI copy.
- [ ] Keep technical identifiers available only where useful for support/debugging.
- [ ] Ensure core configuration tasks work around 360 dp without horizontal scrolling.
- [ ] Prefer stacked cards/sections on compact layouts and progressively expand for desktop.

## 7. Web State / API Integration

- [ ] Inventory data already available to setup, games, draw channels, and seller terminals.
- [ ] Keep API calls in feature stores/services, not presentational components.
- [ ] Use Angular signals and feature-local state.
- [ ] Represent explicit states: loading, loaded, empty, error, and partial/needs-attention where applicable.
- [ ] Do not introduce a new global sales configuration store.
- [ ] Consume `ApiResponse<T>` through API clients and expose unwrapped business data to stores/components.

## 8. Backend Follow-Up Only If Required

- [ ] Start implementation assuming no backend behavior change.
- [ ] Add backend fields only if the UI cannot derive the required presentation reliably.
- [ ] If backend additions are necessary, add read-only BFF/read-model fields only.
- [ ] Use `QueryBus.ask(...)` or stable public APIs for multi-domain aggregation.
- [ ] Do not access repositories, persistence adapters, or another module's internals from `features/*`.
- [ ] Keep typed IDs outside persistence and keep query handlers read-only.

## 9. Validation

- [ ] Run focused admin portal unit tests for touched setup/games/draw-channel components.
- [ ] Run focused lint for touched admin portal project/libs.
- [ ] Run focused admin e2e smoke if runtime is available.
- [ ] Verify responsive screenshots at 360 dp, tablet, and desktop.
- [ ] Verify setup ready, setup incomplete, missing printing, operational card secondary presentation.
- [ ] Verify games active/disabled, POS visibility, stake/pricing status, availability action, and Maryaj Gratis route.
- [ ] Verify draw channels ready+automatic, ready+manual, disabled, no games available, no upcoming draws, manual-not-incomplete, and automatic-not-ready states.
- [ ] Verify seller terminals distinguish tenant print defaults from terminal overrides and preserve supported print modes.
