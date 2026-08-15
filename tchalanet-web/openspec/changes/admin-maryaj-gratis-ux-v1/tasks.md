# Tasks

This checklist tracks the original "Maryaj Gratis UX Simplification & Configuration Refactor"
brief. Checked items are done in the current branch. Unchecked items are still required before the
full brief is complete.

## 0. Discovery / Current Behavior

- [x] Confirm the current Maryaj Gratis page data sources and ownership boundaries.
- [x] Confirm which fields belong to tenant game configuration.
- [x] Confirm which fields belong to Maryaj Gratis-specific offer/campaign configuration.
- [x] Confirm which fields belong to generation/retry configuration.
- [x] Confirm which fields belong to platform/catalog pricing.
- [x] Confirm which fields are actually editable by tenant admins today.
- [x] Confirm priority is campaign/rule arbitration for overlapping or multiple promotions, not primary tenant-admin display data.
- [x] Confirm the far-future end date is a technical representation of "no end date".
- [x] Confirm Exact / Reverse pricing labels come from catalog/platform pricing and are read-only here.
- [ ] Confirm whether Maryaj gratis and regular Maryaj share core game logic as implied by current copy.
- [x] Preserve existing routes and backend behavior.

## 1. Fix Visible UX Bugs First

- [x] Fix raw i18n key leakage such as `admin.gamesPricing.card.standardPricingProfile`.
- [x] Audit Maryaj Gratis main page and edit surfaces for mixed French / Kreyol copy via strict i18n audit.
- [x] Ensure new user-visible labels, descriptions, helper text, validation, and feedback use i18n keys.
- [x] Verify HT / FR / EN bundles contain the new keys.
- [x] Hide backend/internal codes or keep them secondary.
- [x] Stop exposing technical placeholder dates such as `14/08/2036 23:59` in normal admin presentation.
- [x] Stop exposing technical/internal campaign values in primary presentation.
- [x] Move `Priority = 100` to Advanced settings in the offer edit surface.

## 2. Main Maryaj Gratis Page Around 3 Business Sections

- [x] Stop presenting the page as one long summary plus one long form.
- [x] Add a compact `Jwet Maryaj gratis` section.
- [x] Add a compact `Of Maryaj gratis` section.
- [x] Add a compact `Jenerasyon` section.
- [x] Keep game readiness/status visible.
- [x] Keep game code secondary.
- [x] Remove the oversized navy summary card.
- [x] Avoid repeating the same active/ready status in multiple places.
- [x] Keep `Modifye jwet la` distinct from offer configuration.
- [x] Show campaign status, start date, business-facing end state, attribution mode, and tiers.
- [x] Keep Pause/Resume separate from Edit.
- [x] Keep generation/retry behavior visually separate from offer thresholds.
- [x] Move generation edit controls out of the offer editor and into the generation section.
- [x] Show a compact read-only empty state when no Maryaj Gratis offer campaign exists.
- [x] Show a compact read-only empty state when the Maryaj Gratis game is missing.
- [x] Show a compact read-only empty state when Maryaj Gratis generation settings are missing.
- [ ] Move rarely used generation knobs under Advanced settings if product confirms they are not normal tenant-admin controls.

## 3. Replace Long Game Configuration Modal

- [x] Stop treating the full game configuration as a simple modal on compact/mobile layouts.
- [x] Provide a dedicated route/page or full-height responsive drawer/sheet for game editing.
- [ ] Preserve unsaved-change behavior and cancel/save semantics.
- [ ] Keep save actions reachable without trapping the admin in a very long dialog.
- [x] Structure the game editing surface as Vant, Miz, Disponibilite, Opsyon jwet, Barem/Gany, and Opsyon avanse.

## 4. Game Editor - Vant

- [x] Replace verbose repeated copy with one short section description.
- [ ] Clearly distinguish game enabled for tenant from visible/usable on POS.
- [x] Review and simplify current labels such as `Vann jwet sa a` and `Montre jwet sa a nan POS`.
- [ ] Preserve safe quick activation behavior if backend support is valid.
- [ ] Add focused tests for POS visibility toggle and game activation.

## 5. Game Editor - Miz

- [x] Group min/max stake inputs together.
- [x] Use clear HTG suffixes.
- [ ] Format large values for readability, such as `10 000 000 HTG`.
- [ ] Keep structural validation close to fields.
- [x] Avoid duplicating min/max values in multiple sections.
- [ ] Add focused tests for stake min/max validation.

## 6. Game Editor - Disponibilite

- [x] Keep a clear link/action to `Disponibilite pa tiraj`.
- [x] Do not embed the full draw/game matrix in the Maryaj Gratis form.
- [x] Clarify what `Limite le vant yo` means in business language.
- [x] Rename the availability checkbox if it controls participation in draw-specific availability rules.
- [x] Keep draw-level availability owned by the existing availability-by-draw surface.
- [ ] Add focused tests for availability navigation.

## 7. Game Editor - Opsyon Jwet

- [ ] Keep the selection mode field with a concise business label.
- [ ] Explain automatic selection once, not repeatedly.
- [ ] Simplify Exact / Reverse option presentation into compact rows.
- [ ] Keep only controls that tenant admins can actually change.
- [ ] Clarify the difference between offered and POS-visible if both controls are required.
- [ ] Translate all option descriptions to the selected locale.
- [ ] Add focused tests for Exact option enable/disable.
- [ ] Add focused tests for Reverse option enable/disable.
- [ ] Add focused tests for default option selection.

## 8. Game Editor - Barem / Gany

- [ ] Separate payout rule overview from rule editing.
- [ ] Show existing rules first as compact summaries.
- [ ] Open an individual rule editor only when the admin selects a rule.
- [ ] Do not render every payout-rule input expanded by default.
- [ ] Confirm destructive payout-rule deletion.
- [ ] Preserve all current supported payout calculation modes.
- [ ] Keep payout business logic out of the frontend.
- [ ] Add focused tests for payout-rule edit.
- [ ] Add focused tests for payout-rule delete confirmation.

## 9. Offer Editor - Permanent / No-End Date

- [x] Replace distant-date UX with a `Pa gen dat fen` choice.
- [x] Reveal the date picker only when the admin chooses a real end date.
- [x] Map no-end-date selection to the backend representation expected by the current API.
- [x] Show `Pa gen fen` in the main summary instead of the internal stored date.
- [x] Preserve explicit end date behavior when a real end date is selected.

## 10. Offer Editor - Priority

- [x] Move `Priyorite` into `Opsyon avanse`.
- [x] Explain priority only if multiple active offers can overlap.
- [x] Preserve the backend value while the field is collapsed.

## 11. Offer Editor - Attribution Mode

- [x] Keep attribution mode separated from dates and advanced technical configuration.
- [x] Use business-facing labels for supported modes in the new copy.
- [x] Reveal tier controls when tiered attribution is selected.
- [x] Hide tier controls when the selected mode does not use them.
- [ ] Add focused tests for attribution mode changes.

## 12. Rework Tier Configuration / Palye

- [x] Display tiers as simple business rules in read mode.
- [x] Keep tier add/edit/delete capability in the existing form.
- [x] Support one open/editing tier at a time.
- [x] Avoid rendering every tier as three large inputs simultaneously.
- [x] Represent open-ended upper range with a clear UI control instead of relying on blank max helper copy.
- [x] Validate overlapping ranges.
- [x] Validate min <= max.
- [x] Validate positive free-Maryaj quantity.
- [x] Do not reimplement domain-level payout/eligibility rules in the frontend.
- [x] Add focused tests for zero tiers.
- [x] Add focused tests for one tier.
- [x] Add focused tests for multiple tiers.
- [x] Add focused tests for tier add/edit behavior.
- [x] Add focused tests for open-ended final tier.
- [x] Add focused tests for invalid min/max and overlap validation.

## 13. Offer Pause / Resume

- [x] Keep Pause/Resume as a fast operational action outside the full editor.
- [x] Confirm before pausing because it immediately affects customer eligibility.
- [x] Keep current campaign status obvious in the offer section.
- [x] Do not mix campaign active state with game active state.
- [ ] Add focused tests for resume.

## 14. Advanced Settings

- [x] Create a secondary advanced section for priority.
- [x] Preserve advanced values when collapsed.
- [ ] Decide whether retry count, generation strategy details, campaign identifier, technical dates, or low-level selection behavior belong in advanced settings.
- [ ] Add concise help text only where needed.

## 15. Main-Page Summary Cleanup

- [x] Remove redundant repeated statuses.
- [x] Remove the large ID / MG treatment from the primary viewport.
- [x] Keep stable/internal identifiers secondary.
- [x] Avoid a large hero/summary card that consumes an entire mobile viewport.
- [x] Prefer compact section summaries with obvious edit actions.
- [x] Keep missing offer setup as an exceptional state instead of opening the full offer form in the summary view.
- [x] Keep missing game setup as an exceptional state with the shared status treatment.
- [x] Translate game incomplete readiness reasons in the main summary instead of leaking raw keys.
- [x] Use the shared console refresh button instead of a local header action.
- [x] Pilot the shared console section card on the Generation section instead of a local card shell.
- [x] Use the shared console section card on the Maryaj Gratis game section.
- [x] Use the shared console section card on the Maryaj Gratis offer section.
- [x] Use the shared console status pill for Maryaj Gratis readiness and offer status instead of local badge styles.
- [x] Keep only exceptional warnings visually prominent.

## 16. Mobile-First Behavior

- [ ] Verify all Maryaj Gratis edit surfaces at approximately 360 px first.
- [x] Avoid nested card to card to field structures on the main page.
- [ ] Avoid simply stacking desktop form groups in the game and offer editors.
- [ ] Keep touch targets comfortable.
- [ ] Keep save/cancel actions reachable.
- [ ] Ensure sticky action areas do not obscure fields.
- [ ] Ensure mobile viewport does not hide the last form controls.
- [ ] Validate keyboard-open behavior for numeric inputs and selects.
- [x] Use existing project responsive breakpoints and theme tokens in touched styles.

## 17. State Management / API Boundaries

- [x] Keep page/edit state inside the Maryaj Gratis feature store/service.
- [x] Do not introduce a new global store.
- [x] Keep HTTP access in API services/store, not presentational components.
- [x] Preserve explicit loading, loaded, saving, error, dirty, and success states.
- [x] Preserve API notices/errors that are relevant to the admin.

## 18. Backend Follow-Up Only If Necessary

- [x] Do not change backend business semantics in this UX pass.
- [x] Do not move business ownership into the web feature.
- [x] Do not add repository/direct SQL access from feature modules.
- [ ] Add backend read/write fields only if later UX work discovers a missing stable API contract.

## 19. Tests

- [x] Add unit coverage for no-end-date hydration and save mapping.
- [x] Add focused coverage for Maryaj Gratis game summary amount formatting and business labels.
- [x] Add focused coverage for Maryaj Gratis game incomplete/readiness labels and reasons.
- [x] Add focused coverage for Maryaj Gratis offer status labels and no-end-date display helpers.
- [x] Add focused coverage for Maryaj Gratis offer zero, one, and multiple tier read helpers.
- [x] Update Maryaj e2e page object for game, offer, generation panels and hidden `2036`.
- [x] Run strict i18n audit.
- [x] Run admin portal TypeScript check.
- [x] Run admin portal lint.
- [x] Run admin portal unit tests cleanly.
- [x] Run web-e2e lint.
- [x] Run admin-business e2e outside the sandbox; Maryaj path passes, but unrelated setup printing and limits scenarios currently fail.
- [x] Add main page coverage for active game plus active offer.
- [ ] Add main page coverage for active game plus paused offer.
- [ ] Add main page coverage for game incomplete.
- [ ] Add main page coverage for offer incomplete.
- [x] Add main page coverage for permanent/no-end-date offer.
- [ ] Add main page coverage for scheduled future offer.
- [ ] Add main page coverage for ended offer.
- [ ] Add main page coverage for zero and one tiers.
- [x] Add main page coverage for multiple tiers.
- [x] Add focused coverage for automatic generation and manual selection behavior.
- [x] Add focused coverage for missing generation settings in read mode.
- [ ] Add main page coverage for generation error state if supported.
- [ ] Add game editor coverage for POS visibility, activation, stake validation, availability navigation, option toggles, payout edit/delete, save/cancel/dirty behavior.
- [ ] Add offer editor coverage for start date, no end date, explicit end date, attribution mode changes, tier add/edit/delete, open-ended final tier, invalid ranges, pause/resume, and advanced value preservation.
- [x] Add e2e/visual guard for no raw translation keys.
- [ ] Capture 360 px screenshots for main page, game editor, offer editor, and tier editor.
- [ ] Capture desktop screenshots.
- [ ] Verify keyboard-open behavior on mobile.
- [ ] Verify sticky save footer does not cover fields.
- [ ] Resolve local `pnpm nx build admin-portal` deadlock or validate the build in CI.

## 20. Definition Of Done

- [x] Maryaj Gratis no longer feels like one giant form on the main page.
- [x] Main page clearly separates Jwet, Of, and Jenerasyon.
- [x] Game editing clearly separates Vant, Miz, Disponibilite, Opsyon jwet, and Barem.
- [ ] Offer editing clearly separates lifecycle, attribution, tiers, and advanced settings in a mobile-first surface.
- [x] Permanent offers no longer expose a fake far-future date to the admin.
- [x] Priority is no longer a primary field unless product later proves it is required.
- [x] Tiers are readable as business rules before they are editable as form fields.
- [x] No raw i18n keys or mixed-language copy are known on the touched main page after strict audit.
- [x] Existing business rules and ownership boundaries are preserved.
- [ ] Mobile UX is verified at approximately 360 px without excessive nested cards or endless expanded forms.
- [ ] Focused tests, lint, e2e, and build gates are green or have documented unrelated blockers.
