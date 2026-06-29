# web-shared-runtime-entrypoints

## Why

The portal is moving toward multiple deployable web apps. Shared runtime capabilities must be
discoverable and reusable without keeping application-specific code in `apps/tch-portal/src/app/core`.

Error management already proved the need: pages, sections, forms, shell feedback, and API clients
need the same normalized model, but the reusable helpers still live under the current app.

## What

- Add stable `@tch/web/*` entrypoints for runtime web capabilities:
  - `@tch/web/core`
  - `@tch/web/auth`
  - `@tch/web/i18n`
  - `@tch/web/errors`
  - `@tch/web/shell`
- Move reusable error copy/routing helpers into `@tch/web/errors`.
- Keep `@tch/api` as the backend HTTP boundary for contracts, backend client, ProblemDetail,
  ApiResponse, and low-level HTTP mapping.
- Document the split so future portal/admin/platform apps can reuse the same bricks.

## Impact

- Existing app code can import shared error helpers from `@tch/web/errors`.
- App `core` remains a composition/wiring area, not the owner of portable runtime logic.
- Shell/auth/i18n start as explicit entrypoints so follow-up slices can move code without inventing
  new boundaries each time.

## Non-goals

- No new deployable app in this change.
- No complete extraction of the current private shell/auth/i18n stores.
- No backend contract redesign.
- No new Nx project per capability unless a future slice proves a physical project boundary is needed.
