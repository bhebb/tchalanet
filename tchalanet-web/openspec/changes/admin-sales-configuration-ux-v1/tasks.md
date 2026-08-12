# Tasks

## 0. Discovery

- [x] Confirm existing setup page separates required and optional cards.
- [x] Confirm backend readiness keeps `limits`, `commission`, and `subscription` out of readiness rollup.
- [x] Confirm games, draw channels, tenant settings, and seller terminals remain separate ownership surfaces.

## 1. Setup Optional Area

- [ ] Add a visual section boundary for optional/operational setup cards.
- [ ] Add POS / printing as an operational setup card linking to tenant settings print configuration.
- [ ] Keep POS / printing out of required progress and seller-terminal creation blockers.
- [ ] Cover setup card ordering and optional card behavior with focused tests.

## 2. Games Configuration UX

- [ ] Redesign games overview cards around business readiness: active, POS-visible, stakes configured, payout/pricing configured, availability by draw.
- [ ] Restructure the game settings dialog into clear sections: sell this game, stakes, payouts, advanced options.
- [ ] Add payout/stake preview copy where the current API has enough data.
- [ ] Keep Maryaj Gratis as a visible linked action, not a duplicated inline form.
- [ ] Ensure all labels are i18n keys and raw game codes are secondary/fallback only.

## 3. Draw Channels UX

- [ ] Redesign draw-channel cards using business labels: provider, draw, schedule, result mode, sale status.
- [ ] Add next actions for "configure games sold on this draw" and "view generated draws".
- [ ] Surface incomplete states without raw technical codes.
- [ ] Keep provider/channel names from backend display data; fallback to stable code only when no display label exists.

## 4. Tenant Settings and Terminal Handoff

- [ ] Add contextual links from tenant settings print/POS configuration back to seller terminal overrides where useful.
- [ ] Add contextual links from seller terminal details back to tenant default receipt/POS configuration.
- [ ] Keep terminal-specific Bluetooth/device fields owned by seller terminal pages.

## 5. Validation

- [ ] Run focused admin portal unit tests for touched setup/games/draw-channel components.
- [ ] Run focused lint for touched admin portal project/libs.
- [ ] Run focused admin e2e smoke if runtime is available.
- [ ] Verify mobile-width screenshots for setup, games, and draw-channel pages.

