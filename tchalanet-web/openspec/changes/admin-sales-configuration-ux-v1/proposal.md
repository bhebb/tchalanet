# admin-sales-configuration-ux-v1

## Why

Tenant admins need to configure selling without thinking in implementation terms. The current admin
portal already has the right ownership split:

- setup page = readiness/control surface
- tenant settings = global tenant configuration
- games = game activation, POS visibility, stake/pricing configuration
- draw channels = provider/channel/schedule configuration and sale availability
- seller terminals = device-specific operational overrides

The gap is UX clarity. The most important configuration surfaces still feel fragmented and technical
for a non-technical tenant admin. Admins need to answer three questions quickly:

1. Can this tenant sell now?
2. Which games can be sold on which draws?
3. Where do I fix a missing configuration without guessing the right page?

## What Changes

- Improve the existing setup page optional area without changing readiness semantics:
  - keep required/blocking cards focused on sale readiness;
  - group optional/operational cards visually;
  - add a POS / printing operational card that links to tenant settings print configuration.
- Improve the games configuration page:
  - make game cards show sale configuration status in business terms;
  - separate activation/POS visibility, stake limits, payout/pricing, and advanced options;
  - keep Maryaj Gratis visible but route to its dedicated page;
  - expose a clear action to review game availability by draw.
- Improve the draw channels page:
  - use business labels for provider/channel/slot concepts;
  - show whether each channel is sale-ready, manually operated, automatic, disabled, or incomplete;
  - make game availability per draw a first-class next action;
  - surface generated draw coverage when a channel is configured but has nothing sellable yet.
- Keep tenant settings as the owner for global tenant defaults:
  - currency, locale/calendar, receipt/PDF/POS defaults, delivery channels, branding/address.
- Keep seller terminal management as the owner for terminal-specific overrides:
  - Sunmi / generic ESC/POS / PDF, 58mm/80mm/A4, auto-print, Bluetooth printer, test print.

## Impact

- Web-only planning scope unless implementation discovers missing data in existing admin setup,
  games, draw-channel, or seller-terminal APIs.
- No route removal. Existing admin routes stay valid.
- No page-model/public engine change.
- No mobile or backend changes are required by default.
- If backend data is insufficient, follow-up server scope must only add BFF/read-model fields; no
  direct SQL in feature modules.

## Non-Goals

- Do not move tenant settings into setup.
- Do not make optional POS/printing, limits, commission, subscription, or Maryaj Gratis settings
  block tenant readiness.
- Do not merge games, draw channels, and terminal pages into one large form.
- Do not change lottery provider names or draw names for display; use backend/provider display names
  and only fall back to stable codes when labels are missing.
- Do not redesign operations menus beyond making these configuration pages easier to reach after the
  Operations section.

