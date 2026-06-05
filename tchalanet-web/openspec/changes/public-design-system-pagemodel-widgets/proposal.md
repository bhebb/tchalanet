# Change: public-design-system-pagemodel-widgets

## Why

The public web surface needs one coherent design system for ticket verification, results, help, legal pages, and operator orientation.
The current PageModel renderer also needs clearer public-widget expectations before more public pages are added.

This change defines a public-first, mobile-first design direction that is operational rather than generic marketing.
It keeps legal wording cautious and keeps business calculations outside the frontend.

## What changes

- Define Tchalanet public design tokens on top of Material Design 3, with light and dark mode support from the start.
- Establish reusable Angular widget rules for public PageModel pages.
- Define V1 public pages and widgets for ticket verification, results, rules/simulation, help, contact, privacy, and terms.
- Define cautious public wording for verification, results, simulations, news, and operator CTAs.
- Require typed PageModel actions and explicit widget states.
- Update the existing base `tchalanet` theme preset values instead of creating a separate one-off public palette.
- Require stateless, well-sliced Angular presentational components with i18n keys wired from the first implementation.

## Impact

- Touches only `tchalanet-web`.
- Guides public Angular components, public routing, theme CSS variables, and PageModel widget props.
- Requires updates in the existing web theme pipeline under `apps/tch-portal/src/app/core/theme/`.
- Requires public copy to live in the existing i18n assets for fr/en/ht instead of being hardcoded in templates.
- May later require backend/PageModel payload alignment for widget data, result details, rules, and simulations.
- Does not change ticket payout, odds, pricing, or verification business logic.

## Non-goals

- No backend implementation in this change.
- No hardcoded frontend odds, multipliers, payouts, or game pricing.
- No private dashboard navigation changes.
- No custom non-Material theming system.
- No CMS editor or widget authoring UI.
