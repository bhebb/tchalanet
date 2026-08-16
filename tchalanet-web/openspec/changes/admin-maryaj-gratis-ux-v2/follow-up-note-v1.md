# Maryaj Gratis Follow-Up Note

The first UX pass is merged and the direction is good. The game card pattern now works well on
mobile because it is compact, business-oriented, and shared with the normal games configuration
surface.

## Keep

- Keep the shared game card for `Jwet disponib` and the Maryaj Gratis game section.
- Keep game editing on the shared routed editor instead of a modal.
- Keep the page container responsible for loading data, confirmations, and API calls.
- Keep presentational components stateless where possible.

## Remaining Product Questions

- Should the offer summary become a card with the same anatomy as the game card on mobile?
- Should the offer card expose only the current campaign state, the main tier rules, and the primary
  edit/pause actions?
- Should desktop keep the current section-card layout while mobile uses a denser card presentation?

## Proposed Offer Card Direction

Use a compact mobile offer card similar to the game card:

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

- `Modifye` is the normal action and can keep the stronger visual treatment.
- `Mete an poz` stays secondary/outline because it is operational and sensitive.
- Show warnings only for exceptional states: missing offer, ended offer, invalid tiers, or failed
  load/save.
- Do not turn the offer summary back into a dashboard or long form.

## Remaining Work

- Verify the merged display on mobile around 360 px:
  - Maryaj Gratis main page.
  - shared game editor.
  - offer editor.
  - tier editor.
- Decide whether to refactor the offer display into a shared/stateless `MaryajOfferCard` style
  component.
- Keep the existing offer editor route/surface for now; only change display once the card direction
  is validated.
- Re-run the focused web gates after any follow-up:
  - focused unit tests for Maryaj display/editing.
  - i18n audit.
  - breakpoint contract.
  - OpenSpec validation.
  - focused e2e or screenshots for mobile if the browser is available.

