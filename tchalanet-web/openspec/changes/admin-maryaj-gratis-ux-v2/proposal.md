# admin-maryaj-gratis-ux-v2

## Goal

Complete the Maryaj Gratis administration UX by verifying and hardening the mobile layout at 360 px,
deciding on a compact offer card, and closing the remaining test and build gaps from v1.

## Why

The v1 pass (PR #663) established the three-section page structure, the shared game card, the routed
game editor, and the redesigned offer/tier editors. Several items were deferred explicitly in the v1
`tasks.md` and a post-merge follow-up note:

- mobile layout at 360 px was not verified for all four edit surfaces (main page, game editor, offer
  editor, tier editor);
- the offer display is still a heavy panel — the shared game card pattern is validated, and the offer
  section may benefit from the same compact card anatomy on mobile;
- focused game-editor unit tests (POS visibility, activation, stake validation, option toggles,
  payout edit/delete, dirty/cancel) are missing;
- generation error state coverage is missing;
- 360 px and desktop screenshots are not captured;
- the local `pnpm nx build admin-portal` deadlock is unresolved.

## What Changes

### 1. 360 px Mobile Verification

Verify all four Maryaj Gratis edit surfaces at approximately 360 px without excessive nested cards or
stacked full-width form groups:

- main page (three-section overview);
- shared game editor (`admin-game-settings.page`);
- offer editor (`maryaj-offer-panel`);
- tier editor (inline within offer editor).

Fix any layout pressure found: replace stacked desktop groups with responsive two-column rows where
the breakpoint contract allows, ensure the sticky save footer does not cover the last field, ensure
numeric keyboard does not hide critical controls.

### 2. Offer Display — Compact Card Decision

Decide whether `maryaj-offer-panel` read mode should be refactored into a compact stateless
`MaryajOfferCard` with the same anatomy as the shared game card:

```text
Of gratis la                                      Aktif
14/08/2026 -> Pa gen fen
Pa palye

100 HTG - 199 HTG -> 1 Maryaj gratis
200 HTG - 499 HTG -> 2 Maryaj gratis
500 HTG + -> 3 Maryaj gratis

[Modifye]     [Mete an poz]
```

Guidelines:
- `Modifye` keeps the stronger visual treatment (primary/filled).
- `Mete an poz` stays secondary/outline — operational and sensitive.
- Show warnings only for exceptional states: missing offer, ended offer, invalid tiers, failed
  load/save.
- Do not turn the offer summary back into a dashboard or long form.
- Keep the existing offer editor route/surface; change display only after the card anatomy is
  validated at 360 px.

Desktop keeps the current section-card layout; mobile uses the denser card presentation.

### 3. Test Gaps

Close the remaining unchecked test items from v1:

- focused game editor unit tests (POS visibility, activation, stake validation, availability
  navigation, option toggles, payout edit/delete, save/cancel/dirty);
- main page coverage for generation error state;
- 360 px screenshots for main page, game editor, offer editor, and tier editor;
- desktop screenshots;
- keyboard-open behavior verification on mobile.

### 4. Build Gate

Resolve or document the `pnpm nx build admin-portal` deadlock so the build is verifiable locally
or is green in CI.

### 5. Discovery Carry-over

Confirm whether Maryaj gratis and regular Maryaj share core game logic (deferred from v1 section 0).

## Impact

- Web-only change in `admin-portal`.
- No new backend fields unless the 360 px verification reveals a missing stable API contract.
- Existing routes and store remain valid.
- If `MaryajOfferCard` is extracted, it lives in the promotions feature — not a shared lib at this
  stage.

## Non-Goals

- Do not change Maryaj Gratis eligibility, payout, generation, or campaign business rules.
- Do not move campaign ownership into games pricing or game ownership into promotions.
- Do not add backend endpoints or migrations.
- Do not redesign the tier editor form (only verify mobile layout).
- Do not redesign the game editor sections (only verify mobile layout and add unit tests).
