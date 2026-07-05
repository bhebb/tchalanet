# web-console-shared-folders-v1

## Why

`admin-portal` and `platform-portal` both carry local `shared` folders for private console code.
Some of that code is already reused across several console features, while its placement still
suggests app-local ownership.

The current layout makes imports depend on relative paths such as `../../shared/...`, and it blurs
the boundary between:

- console-private reusable code,
- app-owned feature code,
- truly cross-app runtime assets/config.

The placement guide already identifies `libs/web/console` as the home for inter-console business
data-access and console-private reusable web code. This change makes that boundary explicit.

## What

- Audit the two app-local shared areas:
  - `apps/admin-portal/src/app/shared`
  - `apps/platform-portal/src/app/features/shared`
- Move stable console-private reusable code into `libs/web/console`.
- Define the canonical technical display vocabulary for console surfaces:
  game code, bet type, bet option, draw, draw result, draw channel, provider/logo, pricing/barème,
  ticket details, print views, and stats.
- Inventory existing pipes/helpers and decide which ones become the public `@tch/web/console`
  display contract.
- Keep app-owned or route-specific code next to the owning feature when extraction would create a
  fake shared abstraction.
- Update imports and public exports so consuming pages use `@tch/web/console` where appropriate.
- Keep behavior, selectors, route paths, templates, and runtime UX unchanged.
- Validate admin/platform portal compilation and relevant web-console/core tests.

## Impact

- Cleaner ownership for console-only reusable code.
- Fewer deep relative imports from feature pages into app-local `shared` folders.
- `libs/shared-assets` and `libs/shared-config` remain untouched because they are genuinely
  cross-surface assets/config, not console-private code.
- Public portal code remains out of scope.
- Print/detail/stats surfaces can reuse the same display contract instead of re-creating label
  mappings locally.

## Non-goals

- No backend changes.
- No public portal PageModel/widget restructuring.
- No backend enum/model changes; this change consumes existing stable technical codes.
- No design-system migration to `libs/ui/console` unless a component is already a pure UI primitive.
- No route, UX, wording, or permission behavior changes.
- No broad renaming beyond what is necessary for the move.
