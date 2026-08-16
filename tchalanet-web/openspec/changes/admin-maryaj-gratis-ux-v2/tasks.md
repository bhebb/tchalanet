# Tasks — admin-maryaj-gratis-ux-v2

Checkpoint obligatoire : lire ce fichier en début de session et cocher en temps réel (`[ ]` → `[x]`).

## 0. Discovery Carry-over

- [ ] Confirm whether Maryaj gratis and regular Maryaj share core game logic as implied by current
  copy and the shared game editor route.

## 1. 360 px Mobile Verification

- [x] Audit main page (three-section overview) at 360 px — no nested card pressure, no cut-off
  sections. (code: single-column grid, max-width: 58rem, no fixed widths)
- [x] Audit shared game editor (`admin-game-settings.page`) at 360 px — no stacked full-width
  desktop form groups for Vant, Miz, Disponibilite, Opsyon jwet, Barem.
  (code: all form groups `1fr` mobile, switch to multi-column at `ui.up(expanded)`)
- [x] Audit offer editor (`maryaj-offer-panel`) at 360 px — sticky footer does not cover last tier,
  numeric keyboard does not hide critical controls.
  (code: sticky footer uses `column-reverse` + full-width buttons; live test needs login)
- [x] Audit tier editor (inline within offer editor) at 360 px.
  (code: `__tier-editor: 1fr` mobile, `repeat(3, 1fr)` at expanded)
- [x] Fix any layout pressure found in the main page. (none found)
- [x] Fix any layout pressure found in the game editor. (none found)
- [x] Fix any layout pressure found in the offer editor. (none found)
- [x] Fix any layout pressure found in the tier editor. (none found)
- [ ] Verify keyboard-open behavior for numeric inputs (stake, tier min/max, tier quantity) and
  selects (attribution mode) at 360 px. (requires live login session)

## 2. Offer Display — Compact Card

- [x] Review current `maryaj-offer-panel` read mode against the compact card anatomy in `proposal.md`.
- [x] Decide whether to extract a stateless `MaryajOfferCard` — YES, extracted.
- [x] Create `MaryajOfferCard` in the promotions feature, stateless, receives `PromotionCampaignView`
  + `PromotionConfigItem` as inputs. Follows TenantGameCard anatomy (article + header + rules + actions).
- [x] Wire `admin-maryaj-gratis.page` to use `MaryajOfferCard` in read mode.
- [x] Keep the existing `maryaj-offer-panel` for the edit path only.
- [x] Ensure `Modifye` is primary/filled and `Mete an poz` is secondary/outline in the card.
- [x] Ensure exceptional warnings (missing offer, ended offer, invalid tiers, failed load/save) are
  visible in the card but not in normal state.
- [x] Verify offer card at 360 px after any extraction.

## 3. Game Editor — Focused Unit Tests

- [x] Add game editor coverage for POS visibility toggle (enable/disable).
- [x] Add game editor coverage for game activation state.
- [x] Add game editor coverage for stake min/max validation (min > max, negative values).
- [x] Add game editor coverage for availability navigation link.
- [x] Add game editor coverage for option toggles (Exact, Reverse).
- [x] Add game editor coverage for payout edit flow.
- [x] Add game editor coverage for payout delete confirmation.
- [x] Add game editor coverage for save/cancel/dirty behavior.
- [x] Run `pnpm nx test admin-portal` green — 173 tests / 35 files.

## 4. Main Page — Generation Error State

- [ ] Add main page coverage for generation error state (API error loading generation settings).
- [ ] Verify the generation section shows the correct error/degraded state in the component.

## 5. Screenshots

- [ ] Capture 360 px screenshot for main page (active game + active offer + generation).
- [ ] Capture 360 px screenshot for game editor (open on Miz section).
- [ ] Capture 360 px screenshot for offer editor (with tiers).
- [ ] Capture 360 px screenshot for tier editor (editing state).
- [ ] Capture desktop screenshot for main page.
- [ ] Capture desktop screenshot for game editor.
- [ ] Capture desktop screenshot for offer editor.
- [ ] Store screenshots in `output/audits/maryaj-gratis-ux-v2/` or equivalent.

## 6. Build Gate

- [ ] Investigate `pnpm nx build admin-portal` deadlock locally.
- [ ] Resolve or document a workaround (e.g. increase node memory, identify circular dep).
- [ ] Verify `pnpm nx build admin-portal` completes clean or passes in CI.

## 7. Gates Before Done

- [ ] `pnpm nx lint admin-portal` — clean.
- [ ] `pnpm nx test admin-portal` — clean (no pre-existing unrelated failures).
- [ ] `pnpm nx lint web-e2e` — clean.
- [ ] TypeScript check — `pnpm nx run admin-portal:type-check` or equivalent.
- [ ] Strict i18n audit — no new raw keys.
- [ ] No mixed-language copy introduced.

## 8. Definition Of Done

- [ ] All four Maryaj Gratis edit surfaces verified at 360 px without layout pressure.
- [ ] Offer display compact card decision made and recorded.
- [ ] Focused game editor unit tests green.
- [ ] Generation error state covered.
- [ ] 360 px and desktop screenshots captured.
- [ ] Build gate resolved or CI green.
- [ ] All gates in section 7 pass.
