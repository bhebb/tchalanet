# Technical Display Vocabulary Design

## Goal

Create one console-owned display contract for stable technical lottery terms so admin/platform
screens render the same labels and assets in:

- draw lists and draw details,
- draw result lists and result details,
- draw channels and result slots,
- games and pricing/barèmes,
- ticket detail and print/receipt surfaces,
- stats and reports.

These terms are product/domain codes, not page copy. They should not drift screen by screen.

## Existing Inventory

Already in `libs/web/console`:

| Area | Existing API | Notes |
| --- | --- | --- |
| Game name | `consoleGameName`, `ConsoleGameNamePipe` | Handles `HT_BOLET`, `HT_LOTO3`, etc. |
| Game compact text | `consoleGameLogoText`, `ConsoleGameLogoTextPipe` | Used for avatar/logo fallback text. |
| Bet type | `consoleBetTypeLabel`, `ConsoleBetTypeLabelPipe` | Handles `STRAIGHT`, `BOX`, `LOTTO4_PATTERN`, etc. |
| Bet option | `consoleBetOptionLabel`, `ConsoleBetOptionLabelPipe` | Handles option numbers per bet type. |
| Bet display | `consoleBetLabel`, `ConsoleBetLabelPipe` | Safe default when option may be absent. |
| Draw status | `consoleDrawStatusLabel`, `consoleDrawStatusTone` | Used by admin/platform draw tables. |
| Draw sales status | `consoleDrawSalesStatusLabel`, `consoleDrawSalesStatusTone` | Used for sales lifecycle state. |
| Draw result status | `consoleDrawResultStatusLabel`, `consoleDrawResultStatusTone` | Used by result surfaces. |
| Result quality | `consoleDrawResultQualityLabel`, `consoleDrawResultQualityTone` | Quality/source state. |
| Publication status | `consoleDrawPublicationStatusLabel`, `consoleDrawPublicationStatusTone` | Public result visibility state. |
| Draw lifecycle action | `consoleDrawLifecycleActionLabel/Icon` | Operation action labels/icons. |

Already in `libs/web/console`, but still underused:

| Area | Existing API | Notes |
| --- | --- | --- |
| Draw slot identity component | `tch-console-draw-slot-identity` | Can render logo, title, subtitle, local/provider time labels. Current callers still pass pre-composed strings, so it does not yet enforce a consistent provider/channel/date contract. |
| Draw table | `tch-console-draws-table` | Used by admin generated draws and platform ops draws. It accepts stringly rows (`title`, `subtitle`, `meta`, `logo*`) and should move toward a structured draw identity object. |
| Draw results table | `tch-console-draw-results-table` | Used by admin and platform result pages. It already has separate date/provider/local time fields, but draw identity is still mapped differently per app. |

Duplicated app-local helpers:

| Area | Current files | Target |
| --- | --- | --- |
| Lottery provider/slot/channel assets | `apps/admin-portal/src/app/shared/lottery/lottery-assets.ts`, `apps/platform-portal/src/app/shared/lottery/lottery-assets.ts` | Move to `libs/web/console/src/lib/lottery/console-lottery-assets.ts`; asset files remain in `@tch/shared-assets`. |
| Result slot label | `apps/*/src/app/shared/results/result-slot-label.ts` | Move to `libs/web/console/src/lib/draw-slots/console-result-slot-label.ts`. |
| Haiti lot/game mapping | `apps/*/src/app/shared/results/haiti-lot-game-mapping.ts` | Move to `libs/web/console/src/lib/draw-results/console-haiti-lot-game-mapping.ts`. |
| Haiti lots display component | `apps/*/src/app/shared/results/haiti-lots-display.component.ts` | Candidate for `libs/web/console` only if both admin and platform need the same component contract. |

Public/PageModel pipes:

| Area | Existing API | Decision |
| --- | --- | --- |
| PageModel i18n label | `tchLabel` from `@tch/page-model` | Keep for PageModel/public i18n keys. |
| Legacy catalog label | `gameLabel` from `@tch/page-model` | Do not introduce in new console screens; console uses `@tch/web/console` helpers. |

Backend seed/catalog observations:

| Area | Existing source | Observation | Decision |
| --- | --- | --- | --- |
| Game seed | `V204__seed_core_game_draw.sql` | Game codes are stable (`HT_BOLET`, `HT_LOTO3`, ...); names are business display names. Current seed has `Bolèt`; desired UI label is `Bòlèt`. | Keep game code as canonical key. Web helper may normalize `HT_BOLET` to `Bòlèt`; backend seed should be aligned in a follow-up if needed. |
| Bet options | `BetOption` enum | Backend already defines option code, label, description for `MARRIAGE_2D2D`, `LOTTO3_3D`, `LOTTO4_PATTERN`, `LOTTO5_PATTERN`. | Web glossary should mirror backend semantics and eventually be generated/aligned from this catalog contract. |
| Draw channel seed | `V204__seed_core_game_draw.sql` and `DrawChannelProvisioningService` | `draw_channel.name` is seeded as display copy such as `Haïti • New York • Midday`. | Do not treat `name` as canonical technical identity. Prefer `code`, linked `result_slot`, provider, period/time. |
| Result slot seed | `result_slot.slot_key`, `provider`, `draw_time`, `source_cfg` | Slot data is structured and stable enough to derive provider/channel labels and assets. | Console display helpers should derive from this structured data when available. |
| Draw channel formatter | `DrawChannelDisplayFormatter` | Formats `channel.name + time`; currently assumes `name` is already localized/catalog-owned. | Backend follow-up should move composed labels out of seed data or expose structured display fields. |

Responsive draw/result/channel inventory:

| Surface | Current display | Drift / replacement target |
| --- | --- | --- |
| Public latest results | Desktop table plus mobile cards. Uses `drawChannelLabelKey`, `slotKey`, raw `resultDate`, and `hhmm(drawTime)`. Provider filters use local `providerLabel`. | Replace row/card draw identity with shared provider/channel/date formatter. Mobile shows compact provider/logo + date/time first; desktop can show long provider/channel in separate columns. |
| Public result detail | Uses `channelLabel()` and repeats `slotKey · resultDate · drawTime` in hero/card/receipt. | Use the same draw identity as the list so detail, receipt, and related result cards do not invent labels. |
| Admin generated draws | Uses `tch-console-draws-table`, but maps provider logo through app-local lottery helpers and date/time through generated-draw helpers. | Map backend draw data to a structured identity with provider long/short label, official draw date/time, tenant/local time, and logo. |
| Admin draw results | Uses local `PROVIDER_LABELS`, `SLOT_LABELS`, `lotteryLogoForSlot`, and `row.channelName` as preferred title. | Remove local maps. Do not let legacy `Haïti • ...` names dominate. Use provider/slot/channel identity and shared date/time formatting. |
| Platform ops draws | Maps `title` from `draw.channel.name`, `subtitle` from `draw.slot.key`, and `meta` from `draw.channel.code`. | Replace title/meta strings with structured identity derived from code, result slot, provider, draw date, scheduled time, timezone, and logo. |
| Platform ops draw results | Uses `humanizeSlotKey`, local slot label keys, and app-local lottery assets. It has provider/local time formatting but not shared identity. | Reuse the same draw result identity contract as admin and public. |
| Platform catalog draw channels | Raw Material table displays `code`, `name`, `drawTime`, `timezone`, days, order, status. | Introduce a draw-channel list/card/table view that treats `name` as display override/fallback and highlights provider, slot/period, official time, timezone, code, and status. |

## Canonical API Shape

Expose pure functions and standalone pipes from `@tch/web/console`.

Functions are preferred for TypeScript view-model mapping, services, and print payload formatting.
Pipes are preferred for templates.

### Games

- Function: `consoleGameName(gameCode, displayName?)`
- Pipe: `consoleGameName`
- Function: `consoleGameLogoUrl(gameCode)`
- Function: `consoleGameIdentity(gameCode, displayName?)`
- Function: `consoleGameLogoText(gameCode, displayName?)`
- Pipe: `consoleGameLogoText`

Stable examples: `HT_BOLET -> Bòlèt`, `HT_MARYAJ -> Maryaj`, `HT_LOTO3 -> Loto 3`.

Game pages and customer/operator-facing game summaries should display the game name, not the
technical game code. The code remains an internal key, fallback, test fixture, and optional
debug/admin reference. Do not show `HT_BOLET`, `HT_LOTO3`, etc. in the normal game identity block
when a readable name exists.

Game labels are localized display text. Backend/Web technical identifiers may use `LOTTO` or
`lotto3` because those are codes, enum values, route/data ids, or i18n keys, but French UI labels
should render `Loto`, for example `Loto 3`, `Loto 4`, and `Loto 5`. Use `Lotto` only when quoting an
external provider/product name that is officially spelled that way.

Game identity should prefer the dedicated game logos in shared assets before falling back to compact
text. Current assets:

| Game code | Display label | Logo asset |
| --- | --- | --- |
| `HT_BOLET` / `HT_BORLETTE` | Bòlèt | `assets/images/games/ht-bolet.svg` |
| `HT_MARYAJ` | Maryaj | `assets/images/games/ht-maryaj.svg` |
| `HT_MARYAJ_GRATIS` | Maryaj gratis | `assets/images/games/ht-maryaj-gratis.svg` |
| `HT_LOTO3` | Loto 3 | `assets/images/games/ht-loto-3.svg` |
| `HT_LOTO4` | Loto 4 | `assets/images/games/ht-loto-4.svg` |
| `HT_LOTO5` | Loto 5 | `assets/images/games/ht-loto-5.svg` |

The game matrix should render `ConsoleGameIdentity` with logo + readable label. Technical game code
may be available as secondary/debug metadata, but the normal card/matrix identity should not show
the code when a readable name and logo exist.

### Bet Types and Options

- Function/pipe: `consoleBetTypeLabel`
- Function/pipe: `consoleBetOptionLabel`
- Function/pipe: `consoleBetLabel`

`consoleBetLabel` is the default for pricing/barèmes, ticket lines, print lines, and stats when the
UI should prefer the option label and fall back to the bet type.

The source of truth is not the pipe. Bet definitions live in a domain glossary module that exposes
structured entries:

```ts
interface ConsoleBetGlossaryEntry {
  readonly gameFamily: 'BOLET' | 'MARYAJ' | 'LOTO3' | 'LOTO4' | 'LOTO5' | string;
  readonly betType: string;
  readonly betOption?: number | null;
  readonly patternCode?: string | null;
  readonly shortLabel: string;
  readonly longLabel: string;
  readonly description?: string;
}
```

Target placement:

```text
libs/web/console/src/lib/domain/
  console-bet-glossary.ts
  console-bet-glossary.spec.ts
```

The existing helpers in `games/console-game-display.ts` should delegate to this glossary.

### Domain Bet Glossary

The glossary defines backend technical codes and their stable product meaning. Labels below are the
canonical French console labels for now; the helper can later delegate to i18n without changing
consumers.

| Game family | Bet type / pattern | Option | Short label | Long label / meaning |
| --- | --- | --- | --- | --- |
| Bòlèt | `STRAIGHT` | - | Direct | Numéro exact dans l'ordre. |
| Bòlèt | `BOX` | - | Permuté | Numéro gagnant dans n'importe quel ordre autorisé. |
| Bòlèt | `FRONT_PAIR` | - | Deux premiers | Les deux premiers chiffres correspondent. |
| Bòlèt | `BACK_PAIR` | - | Deux derniers | Les deux derniers chiffres correspondent. |
| Bòlèt | `MATCH_1_2D` | - | 1er lot | Pari sur le premier lot. |
| Bòlèt | `MATCH_2_2D` | - | 2e lot | Pari sur le deuxième lot. |
| Bòlèt | `MATCH_3_2D` | - | 3e lot | Pari sur le troisième lot. |
| Maryaj | `MARRIAGE_2D2D`, `PATTERN2X2`, `PATTERN2_2`, `PATTERN2STAR2`, `PATTERN_2X2` | 1 | Ordre exact | Combinaison 2 chiffres + 2 chiffres dans l'ordre exact attendu. |
| Maryaj | `MARRIAGE_2D2D`, `PATTERN2X2`, `PATTERN2_2`, `PATTERN2STAR2`, `PATTERN_2X2` | 2 | Revers / double | Combinaison Maryaj avec revers ou couverture double selon le barème. |
| Loto 3 | `LOTTO3_3D` | 1 | Exact | Trois chiffres exacts dans l'ordre. |
| Loto 3 | `LOTTO3_3D` | 2 | Permuté | Trois chiffres gagnants dans un ordre autorisé. |
| Loto 4 | `LOTTO4_PATTERN` | 1 | Exact | Quatre chiffres exacts dans l'ordre. |
| Loto 4 | `LOTTO4_PATTERN` | 2 | Permuté | Quatre chiffres gagnants dans un ordre autorisé. |
| Loto 4 | `LOTTO4_PATTERN` | 3 | Deux premiers | Les deux premiers chiffres correspondent. |
| Loto 4 | `LOTTO4_PATTERN` | 4 | Deux derniers | Les deux derniers chiffres correspondent. |
| Loto 5 | `LOTTO5_PATTERN` | 1 | 1er + 2e lot | Combinaison des premier et deuxième lots. |
| Loto 5 | `LOTTO5_PATTERN` | 2 | 1er + 3e lot | Combinaison des premier et troisième lots. |
| Loto 5 | `LOTTO5_PATTERN` | 3 | Mixte 1er/2e/3e lot | Combinaison mixte sur les trois lots. |

Fallbacks:

- Unknown known-family bet type: readable technical label.
- Known bet type with unknown numeric option: `Option N`.
- Unknown non-numeric option: raw option string.
- Missing option: return the bet type label.

The glossary is the place to add future patterns, not page components and not pipe classes.

Supported options are product decisions, not only display labels. The web glossary mirrors the
backend support matrix and must not make provider documentation variants appear sellable unless the
backend settlement contract supports them.

Current supported matrix to mirror from backend:

| Game family | Bet type | Option | Label | Settlement support |
| --- | --- | --- | --- | --- |
| Maryaj | `MARRIAGE_2D2D` | 1 | Ordre exact | Supported |
| Maryaj | `MARRIAGE_2D2D` | 2 | Revers / double | Supported |
| Loto 3 | `LOTTO3_3D` | 1 | Exact | Supported |
| Loto 3 | `LOTTO3_3D` | 2 | Permuté / Box | Supported |
| Loto 4 | `LOTTO4_PATTERN` | 1 | Exact | Supported |
| Loto 4 | `LOTTO4_PATTERN` | 2 | Permuté / Box | Supported |
| Loto 4 | `LOTTO4_PATTERN` | 3 | Deux premiers | Supported |
| Loto 4 | `LOTTO4_PATTERN` | 4 | Deux derniers | Supported |
| Loto 5 | `LOTTO5_PATTERN` | 1 | 1er + 2e lot | Supported |
| Loto 5 | `LOTTO5_PATTERN` | 2 | 1er + 3e lot | Supported |
| Loto 5 | `LOTTO5_PATTERN` | 3 | Mixte 1er/2e/3e lot | Supported |

Provider documentation combinations such as `3-way box`, `6-way box`, `4-way box`, `12-way box`,
`24-way box`, or `straight/box` should be modeled as unsupported/deferred unless the backend adds
explicit bet options and settlement tests for them. The helper can expose explanatory metadata for
documentation, but sellable UI must filter to supported options.

The server support matrix separates sale options from computed settlement variants:

- `BetOption` is the sale contract shown to the seller/client.
- `SettlementVariant` is the backend settlement/debug contract.
- web console may display settlement variants only in admin/support/rules contexts.

Target web helpers/components:

- `consoleSettlementVariantLabel(variant)`: admin/support label, e.g. `Permuté · 24-way`.
- `consoleBetVariationRows(resultExplanation | resultFacts)`: formats rows computed from the
  displayed draw result; it must not be a static provider-doc matrix.
- `ConsoleResultCombinationsRulesComponent`: renders the result-derived winning combinations and
  rules for the selected draw/result.

Seller/POS and customer ticket/receipt surfaces must use sale labels only. Admin/support may show
computed variants when backend provides them.

`Combinaisons & règles` is result-bound. It should use the actual result numbers/facts for the draw
being viewed, for example the Pick 3 / Pick 4 / Haiti lots from the selected result. Static rows may
be used only as empty/loading skeleton copy or documentation fallback clearly marked as not tied to
the current result.

### Lottery Assets and Provider Identity

- Function: `consoleLotteryAssetForSlot(slotKey)`
- Function: `consoleLotteryAssetForDrawChannel(drawChannelCode)`
- Function: `consoleLotteryAssetForProvider(providerCode)`
- Function: `consoleLotteryProviderCodeFromSlot(slotKey)`
- Optional pipe wrappers only if templates use them repeatedly.

The functions return URLs under `@tch/shared-assets`. Ownership of image files stays in
`libs/shared-assets/public/assets`.

### Draw Slots and Draw Channels

- Function: `consoleDrawIdentity(input)`
- Function: `consoleDrawChannelIdentity(input)`
- Function: `consoleResultSlotIdentity(input)`
- Component input: structured identity objects, not only pre-composed title/subtitle/logo strings.

Avoid a broad `consoleDrawChannelLabel(channel)` formatter that accepts many shapes and returns one
string. That API would become a magic formatter and hide important display decisions. Builders
should normalize raw inputs into typed identity objects; components and view-model mappers should
then choose explicit fields such as `identity.channelShortLabel`, `identity.channelLongLabel`,
`identity.providerShortLabel`, `identity.providerLongLabel`, and `identity.providerLogoUrl`.

`consoleResultSlotIdentity` should normalize label/provider/time/slot key consistently for draw
result entry, draw result details, and operations dialogs.

`consoleDrawChannelIdentity` should not blindly return backend `draw_channel.name` when that name is
legacy composed copy such as `Haïti • New York • Midday`. Preferred input order for the identity is:

1. stable `code` such as `HT_NY_MID`;
2. linked result slot identity: `slotKey`, `provider`, `drawTime`, `timezone`;
3. structured `period` when available;
4. backend `name` only as fallback or tenant override.

Target display for console screens is provider/period/time without redundant country prefix, for
example:

| Code | Preferred console label |
| --- | --- |
| `HT_NY_MID` | `New York · Midi` or `NY · 14:30` depending surface density |
| `HT_TX_1227` | `Texas · 12:27` |
| `HT_GA_LATE` | `Georgia · Late` / `Georgia · Nuit` |

The `HT_` prefix remains meaningful as a technical family/tenant-lottery code, but it should not
force visible text to start with `Haïti` everywhere.

The identity object should carry both short and long labels so views can choose by density without
recomputing domain rules:

```ts
interface ConsoleDrawIdentity {
  readonly channelCode?: string | null;
  readonly channelShortLabel?: string | null;
  readonly channelLongLabel?: string | null;
  readonly providerCode?: string | null;
  readonly providerShortLabel?: string | null;
  readonly providerLongLabel?: string | null;
  readonly providerLogoUrl?: string | null;
  readonly slotKey?: string | null;
  readonly slotShortLabel?: string | null;
  readonly slotLongLabel?: string | null;
  readonly drawDateLabel?: string | null;
  readonly officialTimeLabel?: string | null;
  readonly officialDateTimeLabel?: string | null;
  readonly officialTimezoneLabel?: string | null;
  readonly localDateLabel?: string | null;
  readonly localTimeLabel?: string | null;
  readonly localDateTimeLabel?: string | null;
  readonly localTimezoneLabel?: string | null;
}
```

Display density rules:

| Viewport / surface | Provider/channel label | Date/time |
| --- | --- | --- |
| Mobile cards | Provider logo or short provider (`NY`, `TX`) plus compact slot/period (`Midi`, `Eve`, `12:27`). Avoid long composed labels in the first line. | Official draw date/time first. Local date/time may be shown as a compact secondary line on admin/operator surfaces. Numbers/status stay prominent. |
| Tablet | Two-column cards or compact rows with provider long label when space allows and short fallback in tight cells. | Official date/time and local date/time remain distinct from fetched/applied timestamps. |
| Desktop table | Long provider/channel label (`New York · Midi`) plus code in secondary text when useful. | Separate official draw date/time and local/tenant date/time columns or stacked labels when the surface needs both. |
| Print / receipt | Long provider/channel label, stable game/bet labels, official draw date/time, local/tenant date/time when relevant, and short code only as reference. | No responsive abbreviations; prioritize clarity and auditability. |

Verification sample:

| App surface | Example input shape | Canonical identity |
| --- | --- | --- |
| Public results | `slotKey=NY_MID`, `channelCode=NY_MID`, official draw date/time | `NY`, `New York · Midday`, New York logo, official date/time |
| Admin generated draws/results | same slot/channel plus optional legacy `channelName=Haïti · New York · Midday` and tenant-local date/time | same provider/channel identity, with local date/time shown as the admin/operator secondary time |
| Platform ops/catalog | `providerCode=NY`, `slotKey=HT_NY_MID`, `channelCode=HT_NY_MID`, optional legacy channel name | same provider/channel identity; `HT_` remains a technical prefix and does not become visible country copy |

This sample is intentionally checked by `consoleDrawIdentity` tests so mobile, tablet, desktop, and
print surfaces choose different fields from the same identity object instead of building separate
labels per app.

Admin result surfaces currently need special attention:

- `ConsoleDrawResultRow` has `occurredDateLabel`, `providerTimeLabel`, and `localTimeLabel`, but no
  explicit `localDateLabel`.
- `AdminDrawResultsPage` currently maps most timing into `meta` via
  `generatedDrawProviderAndTenantTimeLabel`, which combines provider and tenant times without a
  separate local date.
- The replacement should expose official/provider date + time and local/tenant date + time as
  separate display fields so the table can show a local date when the timezone conversion crosses a
  day boundary.

### Draws and Draw Results

Existing status helpers remain under `libs/web/console/src/lib/draws`.

Draw list and detail rendering follow the same pattern:

- page/data-access maps its backend DTO into a console view model;
- `tch-console-draws-table` renders list rows for generated, ops, or future draw sources;
- `tch-console-draw-detail` renders the shared draw detail shell from `ConsoleDrawDetailView`;
- app-owned sections can still be projected into the shared detail when their data is feature-local,
  for example admin financial activity or operational links.
- specialized operational pages that keep a feature-owned table, such as draw lifecycle actions, still
  render the primary draw cell through `tch-console-draw-slot-identity` and use the same identity for
  action feedback/dialog titles.

The shared detail component receives actions, sections, result state, aside metrics, and a structured
draw identity. This lets admin and platform pass different draw DTOs without forking the visual
contract for identity, result summary, facts, action placement, and aside follow-up.

Use the shared builders to keep the page boundary thin:

- `consoleDrawRowViewModel(input)` builds `ConsoleDrawRow` from an identity input plus page-owned
  status/action labels.
- `consoleDrawDetailViewModel(input)` builds `ConsoleDrawDetailView` from the same identity contract
  plus page-owned facts, result state, sections, and aside metrics.
- `consoleDrawResultRowViewModel(input)` builds `ConsoleDrawResultRow` from an identity input plus
  page-owned status, quality, source, timing, and action labels.
- `consoleDrawResultSummaryViewModel(input)` and `consoleDrawResultSummaryFacts(input)` build the
  shared draw-result detail summary contract.

These builders are not backend DTO adapters. Admin/platform/public pages still decide how to read
their DTOs; the builders only enforce the final console view-model shape and fallback behavior.
Public result pages use the summary builder through a public-owned adapter so they share provider,
slot, logo, and date fallback semantics without moving public layout or translation ownership into
`@tch/web/console`.

Ticket and receipt surfaces follow the same rule when the payload carries structured draw fields:

- `consoleTicketDrawIdentity(input)` builds receipt-safe draw identity from `resultSlotKey`,
  `channelCode`, draw date, scheduled time, and a legacy channel label fallback.
- POS open-draw selection and the local ticket preview receipt use this helper because
  `/tenant/cashier/draws/available` already exposes `resultSlotKey`.
- Public ticket verification, persisted ticket detail, and server-generated PDF print receipts must
  not parse `channelLabel`/`drawChannelLabel`; they need a backend contract follow-up to expose the
  same structured draw identity fields before they can be migrated cleanly.

If templates need direct pipes later, add:

- `ConsoleDrawStatusLabelPipe`
- `ConsoleDrawResultStatusLabelPipe`
- `ConsoleDrawPublicationStatusLabelPipe`

Do not add pipes until there is actual repeated template usage; functions are enough for view models.

### Haiti Lot Mapping for Provider Result Entry

Manual and override result entry must show how the provider result maps back to the Haiti lottery
shape before the operator types the values. This is a domain display contract, not decorative help.

Target builder:

```ts
interface ConsoleHaitiLotProviderMapping {
  readonly lotKey: 'lot1' | 'lot2' | 'lot3';
  readonly haitiLotLabel: string;
  readonly providerCode: string;
  readonly providerShortLabel: string;
  readonly providerLongLabel: string;
  readonly providerGameKind: 'pick3' | 'pick4';
  readonly providerGameShortLabel: string;
  readonly providerGameLongLabel: string;
  readonly providerGameLogoUrl: string;
  readonly providerGameLogoAlt: string;
}
```

Builder placement:

```text
libs/web/console/src/lib/draw-results/
  console-haiti-lot-provider-mapping.ts
  console-haiti-lot-provider-mapping.spec.ts
```

Provider mapping rule:

| Haiti lot | Provider game kind | Example NY | Value shape |
| --- | --- | --- | --- |
| `lot1` | `pick3` | `Numbers` | 3 digits |
| `lot2` | `pick4` | `Win 4` | last 2 digits of the provider Pick 4 / equivalent result |
| `lot3` | `pick4` | `Win 4` | last 2 digits of the provider Pick 4 / equivalent result |

Known provider game labels and logo URLs should be centralized with the lottery assets, not copied
inside manual/override dialogs. Current app-local mappings include:

| Provider | Pick 3 label | Pick 4 label |
| --- | --- | --- |
| CA | Daily 3 | Daily 4 |
| FL | Cash 3 | Play 4 |
| GA | Midday 3 | Midday 4 |
| IL | Midday 3 | Midday 4 |
| MI | Daily 3 | Daily 4 |
| MN | Daily 3 | Pick 4 |
| MS | Cash 3 | Cash 4 |
| NY | Numbers | Win 4 |
| TN | Cash 3 | Cash 4 |
| TX | Pick 3 | Daily 4 |
| WI | Pick 3 | Daily Pick 4 |

Every provider listed in the result-slot catalog must resolve:

- a provider identity;
- a Pick 3 / Pick 4 provider game label when applicable;
- a provider game logo URL;
- accessible logo alt text;
- deterministic fallback labels and fallback Pick 3 / Pick 4 logos when the provider is unknown.

Manual and override dialogs should use the same component, for example
`ConsoleHaitiLotMappingComponent`, so the cards shown during result entry match source-result,
draw-result detail, and audit/review surfaces.

Access semantics:

- Manual completion is allowed for tenant/admin operators with manual result-entry capability when
  the draw result is missing or incomplete and the manual-entry delay has elapsed.
- Confirm and override are protected actions for super admin or explicit platform operations
  capability.
- Source/raw provider payload display is an admin/support/platform-ops concern. Public, POS,
  ticket, and customer-facing surfaces should consume the normalized result and shared display
  identities without exposing raw source payloads.

### Pricing / Barèmes

Pricing/barème rows use:

- `consoleGameName`
- `consoleBetLabel`
- `consoleBetTypeLabel`
- `consoleBetOptionLabel`

No page should maintain a separate bet label table for barèmes.

### Tickets, Print, Stats

Ticket detail, ticket print/receipt, and stats should use the same game/bet/draw/provider helpers.

Expected display rules:

- Ticket line game: `consoleGameName(gameCode, displayName?)`
- Ticket line bet: `consoleBetLabel(betType, betOption)`
- Draw identity: provider/logo/channel/slot helpers from `@tch/web/console`
- Draw result lot labels: Haiti lot mapping helper from `@tch/web/console`
- Unknown technical code: readable fallback, never blank unless the source is absent.

## Placement

Use these targets:

```text
libs/web/console/src/lib/games/
  console-game-display.ts
  console-game-labels.pipe.ts

libs/web/console/src/lib/lottery/
  console-lottery-assets.ts

libs/web/console/src/lib/draw-slots/
  console-result-slot-label.ts
  console-draw-slot-identity.*

libs/web/console/src/lib/draw-results/
  console-haiti-lot-game-mapping.ts
  console-draw-results-table.*

libs/web/console/src/lib/pricing/
  console-pricing-*.*
```

Keep app-local feature orchestration where it belongs. For example, a dialog that starts support
access remains platform-owned unless the same dialog contract is reused by another console feature.

## I18n Position

These labels are stable technical display terms. Prefer deterministic helpers over feature-local
translation keys for:

- game codes,
- bet type codes,
- bet option numbers/patterns,
- draw lifecycle statuses,
- draw result statuses,
- provider/channel asset mapping.

Use i18n for surrounding page copy, actions, explanations, errors, and non-technical business text.

If a stable term must become locale-specific, the helper remains the single facade and can later
delegate to i18n without changing consuming pages.

## Backend Follow-up Candidate

This web change can centralize display, but the seed data currently persists labels that look like
UI copy. A backend follow-up should consider:

- changing default `draw_channel.name` from `Haïti • New York • Midday` to a neutral provider/slot
  label (`New York Midday`, `Texas 12:27`, etc.), or
- adding/exposing structured fields (`provider`, `period`, `slotKey`, `labelKey`) so web/print never
  has to parse `name`, or
- returning a stable `displayCode`/`labelKey` contract and treating `name` as tenant override only.

Avoid destructive migration of tenant-customized channel names without a compatibility plan.
