# Design

## References Checked

- `tchalanet-web/docs/conventions/feature-playbook.md`
- `tchalanet-web/docs/conventions/i18n.md`
- `tchalanet-web/docs/conventions/style.md`
- `tchalanet-web/docs/conventions/theme.md`
- `tchalanet-web/docs/conventions/error-management.md`
- `tchalanet-web/openspec/specs/admin-maryaj-gratis-resilience/spec.md`

## Data Ownership

Maryaj Gratis remains a composition of two owning domains:

| Concern                                                                              | Source                                                                             | Current edit owner                           |
| ------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------- | -------------------------------------------- |
| Tenant game activation, POS visibility, stake limits, payout/pricing rows, readiness | `AdminGamesPricingApiService.getGamesPricing()`                                    | Games pricing feature / `GameSettingsDialog` |
| Campaign lifecycle, priority, status, dates, rules                                   | `AdminPromotionsApiService.listCampaigns()` / `getCampaign()` / `updateCampaign()` | Promotions / Maryaj Gratis store             |
| Attribution tiers, quantity mode, selection mode, generation strategy, retry count   | Promotion rule effect params                                                       | Promotions / Maryaj Gratis store             |
| Catalog/platform pricing labels and option names                                     | Games pricing/catalog data                                                         | Read-only summary in Maryaj Gratis           |

The page may summarize cross-domain data, but it must not move write ownership into a presentational
component.

## Target Page Structure

```text
tch-admin-page-shell
  actions: Refresh
  loading | page error | section errors | ready

  Jwèt Maryaj gratis
    readiness
    stake range
    payout/pricing options
    draw availability link
    [Modifye jwèt la]

  Òf Maryaj gratis
    campaign status
    start date
    end state: Pa gen fen OR explicit date
    attribution mode
    readable tier rules
    [Modifye òf la] [Mete an poz / Aktive]

  Jenerasyon
    selection mode
    regeneration/retry behavior
```

The first pass removes the large summary/sidebar and uses a single-column stack so the same mental
model works on mobile and desktop. A future shared console-section primitive may replace the local
section styles when the same pattern is reused elsewhere.

## Error And Empty States

- Initial loading uses `tch-loading`.
- Blocking campaign load failures use `tch-error-panel` with retry.
- Optional game configuration failures use `tch-section-error`, while the campaign surface remains
  usable.
- Campaign detail enrichment/action failures use `tch-section-error`.
- Missing tenant game is an empty section message in the game section.
- Missing campaign is an offer setup/activation state, not a blank page.
- Save/activate/pause actions expose `saving` and disable conflicting actions.

This matches `error-management.md`: page-owned failures stay page-owned, optional degraded slices
stay section-owned, and shell feedback is not duplicated.

## Offer Editing

The first pass keeps the offer editor inline but separates:

- lifecycle: start date, no end date / explicit end date;
- advanced: priority;
- attribution: quantity mode, tiers, per-amount, fixed;
- generation controls.

Permanent offers are represented with a boolean UI control. The store maps `noEndDate=true` to the
current backend-compatible far-future `endsAt` value; admins never need to see or understand the
technical representation.

Advanced settings decision for this pass:

- `priority` stays in the offer advanced section because it only matters when multiple promotions
  can overlap.
- retry count stays in the Generation section, because it directly answers how free Maryaj numbers
  can be regenerated before confirmation.
- `generationStrategy` remains internal. The current UI exposes automatic vs seller selection as
  business language, and the store maps automatic selection to the backend `RANDOM` strategy.
- campaign identifiers, backend-compatible far-future dates, and other technical campaign values
  are not exposed in the tenant-admin editor.
- low-level selection behavior is represented by the Generation section toggle, not by an advanced
  technical field.

## Tier Editing

Read mode shows business rules:

```text
100-199 HTG -> 1 Maryaj gratis
200-499 HTG -> 2 Maryaj gratis
500 HTG+ -> 3 Maryaj gratis
```

Edit mode currently preserves the existing expanded tier rows. The one-tier-at-a-time editor,
open-ended control, and more compact mobile tier editor are intentionally tracked as remaining work
in `tasks.md`.

## Game Editing

The game section summarizes game readiness and routes to the shared games-pricing editor page at
`/app/admin/games/:gameCode/settings`. The editor page owns loading, saving, cancellation, and API
mapping, while the form body is rendered by the stateless shared game settings editor component used
by both the general games configuration flow and Maryaj Gratis.

## Styling And Theming

- Feature-local classes use the existing Maryaj block names and BEM-like elements.
- Styles consume `--tch-*` theme roles and component-local layout only; no new global theme tokens
  are introduced.
- The page does not hardcode brand colors outside existing component/token patterns.
- Responsive layout stays single-column for this first pass to avoid mobile nested-card overload.

## I18n

- All new user-visible copy uses keys under `admin.maryajGratis.*` in
  `feature-admin.json` for HT/FR/EN.
- Removed obsolete permanent-date helper copy.
- Raw backend/internal codes remain secondary or hidden.
- `python3 scripts/i18n-audit.py --project web --strict` is the required gate.

## Validation Strategy

- Unit tests cover store behavior for tier hydration/save, optional game failure, no-end-date
  mapping, and explicit end-date preservation.
- E2E page object checks the three Maryaj panels and ensures the technical `2036` date is not shown
  in the offer panel.
- Focused admin e2e run currently shows Maryaj passing inside the business spec, while unrelated
  setup/limits tests fail and remain out of this change.
- Production build is blocked locally by an `esbuild` deadlock; TypeScript compilation and unit tests
  pass.
