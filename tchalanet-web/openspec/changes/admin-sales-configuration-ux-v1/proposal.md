# admin-sales-configuration-ux-v1

## Goal

Make tenant sales configuration understandable for non-technical admins without changing the current
ownership boundaries.

Admins should be able to answer quickly:

1. Can I sell now?
2. What can I sell, and on which draws?
3. Where do I fix missing configuration?

## Why

Tenant admins need to configure selling without thinking in implementation terms. The current admin
portal already has the right ownership split:

- setup page = readiness/control surface;
- tenant settings = global tenant defaults;
- games = game activation, POS visibility, stake limits, pricing/payout configuration;
- draw channels = tenant-facing draw/result-source/sale-availability configuration;
- seller terminals = POS identity and device-specific operational overrides.

The gap is UX clarity. The most important configuration surfaces still feel fragmented and technical
for a non-technical tenant admin. The work should improve comprehension and corrective routing
without moving settings between domains.

## What Changes

- Setup page:
  - keep existing blocking/readiness semantics unchanged;
  - visually separate required readiness from operational setup;
  - add POS & Printing as operational guidance, not a blocker;
  - ensure every setup problem has one primary corrective destination.
- Games page:
  - redesign game cards around business sale configuration;
  - show Ready / Needs attention / Disabled;
  - make availability on draws first-class;
  - keep Maryaj Gratis visible while routing to its dedicated page.
- Draw channels page:
  - use business labels such as result source, automatic/manual results, draw time, sales close,
    available games, upcoming draws;
  - model sale readiness separately from result-source mode;
  - detect no upcoming draws and no games available as different needs-attention conditions.
- Tenant settings:
  - keep tenant-wide defaults centralized: currency, locale/calendar, receipt, PDF, POS/printing,
    delivery channels, branding, address;
  - improve links so admins land directly in the relevant section.
- Seller terminals:
  - keep terminal-specific printer/POS configuration on seller terminal management;
  - make tenant default vs terminal override visible.
- Shared UX:
  - one problem maps to one destination;
  - remove implementation terminology from primary admin copy;
  - keep responsive admin usability at 360 dp, tablet, and desktop.
- Web integration:
  - keep HTTP/API calls in feature stores/services, not presentational components;
  - use feature-local signal state;
  - do not introduce a global sales-configuration store.

## Impact

- Web-only planning scope unless implementation discovers missing data in existing admin setup,
  games, draw-channel, or seller-terminal APIs.
- No route removal. Existing admin routes stay valid.
- No page-model/public engine change.
- No mobile or backend changes are required by default.
- If backend data is insufficient, follow-up server scope must only add BFF/read-model fields; no
  direct SQL, repositories, or persistence adapter access from feature modules.

## Non-Goals

- Do not move tenant settings into setup.
- Do not make optional POS/printing, limits, commission, subscription, notifications, or Maryaj
  Gratis settings block tenant readiness.
- Do not merge games, draw channels, and terminal pages into one large form.
- Do not change lottery provider names or draw names for display; use backend/provider display names
  and only fall back to stable codes when labels are missing.
- Do not redesign operations menus beyond making these configuration pages easier to reach after the
  Operations section.
- Do not combine automatic/manual result source, ready/needs-attention sale status, disabled state,
  and incomplete configuration into one overloaded enum.
- Do not add backend behavior changes unless missing read data makes them necessary.
