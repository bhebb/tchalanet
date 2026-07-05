# Tasks: web-console-shared-folders-v1

## 0. Discovery and ownership

- [x] 0.1 Identify the candidate app-local shared folders and confirm this is a web-only change.
- [ ] 0.2 Classify each candidate file as one of: move to `libs/web/console`, keep app-owned,
      move to a feature boundary, or leave out of scope.
- [ ] 0.3 Confirm no `public-portal`, `libs/shared-assets`, or `libs/shared-config` move is needed.
- [ ] 0.4 Inventory existing technical display helpers/pipes for games, bet types, bet options,
      draw statuses, draw result statuses, result-slot labels, provider logos, channel logos,
      ticket details, print views, pricing/barèmes, and stats.
- [ ] 0.4a Inventory dedicated game logo assets for Bòlèt, Maryaj, Maryaj gratis, Loto 3, Loto 4,
      and Loto 5, and confirm which game matrix surfaces should render them.
- [x] 0.5 Define the canonical display API names and placement for each stable technical term.
- [ ] 0.5a Define `consoleGameIdentity` / `consoleGameLogoUrl` so game matrix cards use logo +
      readable label and fall back to compact text only when the logo is unavailable.
- [ ] 0.6 Define the domain bet glossary: game code, bet type, bet option, pattern meaning,
      short label, long label, and fallback behavior.
- [ ] 0.6a Align the web bet option glossary with the backend support matrix; provider
      documentation combinations must not appear sellable unless backend settlement supports them.
- [x] 0.6b Align web helper terminology with server `SettlementVariant`: sale labels are normal UI,
      computed variants are admin/support/rules metadata only.
- [ ] 0.7 Record backend seed/catalog observations for games, bet options, draw channels, and result
      slots; decide what web can fix now versus what needs a backend follow-up.
- [x] 0.8 Inventory draw, draw-result, and draw-channel display in public results, public result
      detail, admin generated draws, admin draw results, platform ops draws, platform ops draw
      results, and platform catalog draw channels.
- [ ] 0.9 Audit game/bet translations so technical `LOTTO`/`lotto*` identifiers render as French
      `Loto` labels in UI copy, unless an official external provider/product name requires
      `Lotto`.
- [ ] 0.10 Inventory provider Pick 3/Pick 4 product labels and logos used for Haiti lot mapping in
      manual result, override result, source-result, and draw-result detail surfaces.

## 1. Move console-private shared code

- [ ] 1.1 Move reusable admin/platform console helpers and components into `libs/web/console`.
- [x] 1.2 Add or update `libs/web/console` public exports.
- [ ] 1.3 Update admin-portal imports to use the new ownership boundary.
- [ ] 1.4 Update platform-portal imports to use the new ownership boundary.
- [ ] 1.5 Remove empty app-local shared folders left by the move.
- [ ] 1.6 Consolidate duplicated lottery/provider/result-slot helpers currently living under
      admin/platform local shared folders.
- [x] 1.7 Introduce `consoleDrawIdentity`, `consoleDrawChannelIdentity`, and
      `consoleResultSlotIdentity` builders that derive typed identities from code/result-slot/provider
      data and treat legacy `draw_channel.name` as fallback or tenant override.
- [ ] 1.8 Ensure draw/channel/result-slot labels are read from identity fields instead of broad
      string formatter helpers.
- [x] 1.9 Introduce a structured draw identity model with provider short/long labels,
      provider logo, channel short/long labels, slot short/long labels, draw date, official/provider
      date/time, local/tenant date/time, timezone labels, and stable code fallback.
- [ ] 1.10 Move duplicated Haiti lot provider mapping from admin/platform shared folders into
      `@tch/web/console`, including provider game labels, logo URLs, alt text, and fallback Pick
      3/Pick 4 assets.
- [ ] 1.11 Add focused tests proving each known provider resolves lot1/lot2/lot3 mapping labels and
      logos, and unknown providers resolve fallback labels/logos without blanks.

## 2. Guardrails

- [ ] 2.1 Preserve component selectors, templates, styles, and route behavior.
- [ ] 2.2 Avoid moving app-specific page placeholders or feature-owned dialog orchestration unless
      reuse/ownership is clear.
- [ ] 2.3 Keep `libs/shared-assets` and `libs/shared-config` unchanged.
- [x] 2.4 Keep stable technical-code labels deterministic and local to `@tch/web/console`; use i18n
      only for user-facing copy that is not a stable technical term.
- [ ] 2.5 Ensure print, ticket detail, draw, draw result, draw channel, pricing, and stats surfaces
      can all use the same helpers/pipes instead of local mappings.
- [ ] 2.6 Ensure bet options are defined from the domain glossary rather than directly inside pipe
      classes or page components.
- [ ] 2.6a Ensure sellable option lists use supported option metadata, not provider documentation
      screenshots/examples.
- [x] 2.6b Do not show settlement variants such as 3-way/6-way/12-way/24-way in seller/POS or
      customer receipt flows by default.
- [x] 2.7 Do not make the visible `Haïti • ...` draw-channel seed names canonical in web display
      helpers; prefer stable codes and structured result-slot/provider fields.
- [ ] 2.8 Keep public portal copy/i18n ownership intact while aligning public result display with the
      shared draw identity rules; do not move public-only page composition into `@tch/web/console`.
- [x] 2.9 Do not surface `Lotto` as the French Tchalanet game label; display `Loto` while keeping
      `LOTTO`/`lotto*` only as technical ids, enum values, route/data ids, or i18n key names.
- [x] 2.10 Display `HT_BOLET` as `Bòlèt` in customer/operator-facing UI; keep `Borlette` only as an
      optional French explanatory synonym outside the canonical game identity, and align or
      normalize any existing `Bolèt` seed/display value.

## 3. Responsive draw/result/channel display migration

- [ ] 3.1 Define display density rules for draw identity:
      mobile cards use logo/short provider + compact slot/period + official draw date/time plus
      local date/time on admin/operator surfaces; tablet uses compact rows/cards; desktop tables use
      long provider/channel labels and separate official/local date-time columns or stacks.
- [ ] 3.2 Update `tch-console-draw-slot-identity`, `tch-console-draws-table`, and
      `tch-console-draw-results-table` to accept structured identity data instead of relying only
      on pre-composed `title`, `subtitle`, `meta`, and `logoText` fields.
- [ ] 3.3 Replace public latest-results list/card mappings so provider filters, table rows, mobile
      cards, and date/time text use the same provider/channel/date identity semantics.
- [ ] 3.4 Replace public result-detail hero, metadata card, receipt, and related result labels with
      the same identity semantics as the public list.
- [ ] 3.5 Replace admin generated-draw mappings to use the shared identity helper and remove
      app-local lottery asset imports.
- [x] 3.6 Replace admin draw-results mappings to remove local `PROVIDER_LABELS`, `SLOT_LABELS`, and
      legacy `channelName` as the preferred title.
- [ ] 3.6a Update admin draw-results rows to populate official/provider date/time and local/tenant
      date/time separately; show the local date when timezone conversion crosses a day boundary.
- [ ] 3.7 Replace platform ops-draw mappings that currently use `draw.channel.name` as title with
      provider/channel/date identity derived from code, result slot, provider, scheduled time, and
      timezone.
- [ ] 3.8 Replace platform ops draw-results mappings that currently use local `humanizeSlotKey`,
      label-key generation, and app-local lottery assets.
- [ ] 3.9 Replace platform catalog draw-channel raw table display with a mobile-first channel
      identity row/card layout that highlights provider, slot/period, official time, timezone,
      code, status, and actions.
- [ ] 3.10 Replace manual-result and override-result dialogs so both use the same shared Haiti lot
      mapping component before `lot1`, `lot2`, and `lot3` inputs.
- [ ] 3.11 Ensure result-entry mapping cards show provider identity, provider game label, Haiti lot
      label, and provider game logo on mobile and desktop.
- [ ] 3.12 Verify the same sample draw channel renders consistently across public, admin, and
      platform at mobile, tablet, and desktop widths.
- [ ] 3.13 Update game matrix/card surfaces to use dedicated game logos from `@tch/shared-assets`
      through `@tch/web/console` game identity helpers.
- [x] 3.14 Add `consoleSettlementVariantLabel` for admin/support labels when backend exposes a
      computed settlement variant.
- [x] 3.15 Add `consoleBetVariationRows` as a formatter for backend/result-derived explanation
      rows; it must not generate a static provider-doc matrix without the selected result facts.
- [x] 3.16 Add `Combinaisons & règles` only as a secondary result/detail tab or support view; it
      must use the current draw/result numbers and must not clutter the main result table.

## 4. Validation

- [x] 4.1 Run `pnpm exec tsc -p apps/admin-portal/tsconfig.app.json --noEmit`.
- [ ] 4.2 Run `pnpm exec tsc -p apps/platform-portal/tsconfig.app.json --noEmit`.
- [x] 4.3 Run focused tests/lint for touched libraries when available.
- [x] 4.4 Add focused unit tests for new or moved display helpers/pipes.
- [x] 4.5 Run `git diff --check`.
