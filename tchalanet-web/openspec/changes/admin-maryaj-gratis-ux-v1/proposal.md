# admin-maryaj-gratis-ux-v1

## Goal

Make Maryaj Gratis administration understandable as three separate business concerns:

1. whether the Maryaj Gratis game can be sold;
2. how the free-offer campaign is configured;
3. how free Maryaj selections are generated or awarded.

The UX must reduce technical exposure without changing the existing backend business rules or
ownership boundaries.

## Why

The current Maryaj Gratis page mixes tenant game configuration, campaign lifecycle, attribution
rules, tier thresholds, generation behavior, and technical fields in one long surface. Tenant admins
need a compact status page first, then focused edit surfaces for the game and the offer.

The most visible problems are:

- raw or technical values leaking into the UI, including translation keys and far-future dates;
- duplicated status and summary content;
- priority and generation knobs presented as primary settings;
- tier rules shown as a technical form before the admin can read the business rules;
- mobile layout pressure from long cards and modal-style editing.

## What Changes

- Main page:
  - replace the large summary/sidebar with compact business sections;
  - show game setup, free-offer setup, and generation separately;
  - keep loading, blocking page errors, section degradation, action errors, and empty campaign/game
    states explicit.
- Game section and editor:
  - show stake, payout/pricing option summary, readiness, and availability as a compact summary;
  - keep the game editor owned by the games configuration feature;
  - use the shared routed games configuration editor for the full game form.
- Offer section and editor:
  - show start/end, campaign status, attribution mode, and tier rules as readable business copy;
  - map permanent offers to `Pa gen fen` / equivalent locale copy instead of exposing the backend's
    far-future date representation;
  - keep pause/resume separate from edit;
  - move priority into advanced settings.
- Tier configuration:
  - show tiers as business rules before the form;
  - preserve existing form behavior and validation in this pass;
  - plan a follow-up one-tier-at-a-time editor.
- i18n and conventions:
  - use shared fallback bundles for HT/FR/EN;
  - remove raw key leakage and obsolete helper copy;
  - keep styles local, BEM-like, token-driven, and compatible with `style.md` / `theme.md`;
  - keep API access in the feature store/services, not presentational components.

## Impact

- Web-only change in `admin-portal`.
- Existing routes remain valid.
- Existing promotion and game APIs remain the source of truth.
- No backend semantic change is required by this UX pass.
- Games configuration editing now uses a shared routed page instead of the former compact dialog.

## Non-Goals

- Do not change Maryaj Gratis eligibility, payout, generation, retry, or campaign business rules.
- Do not move campaign ownership into games pricing or game ownership into promotions.
- Do not embed the draw/game matrix in the Maryaj Gratis page.
- Do not make priority a required daily-admin concept.
- Do not invent frontend payout or eligibility calculations.
- Do not change unrelated games-pricing behavior beyond routing game settings to the shared editor.
