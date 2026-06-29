# Design

## Boundary

`libs/shared-assets` owns shared static browser assets and stable URL helpers.

It contains:

- `public/assets/**`: files copied into app browser outputs.
- `src/lib/**`: TypeScript constants and helpers for stable asset URLs.

It does not contain:

- i18n runtime stores or ngx-translate loaders;
- runtime config secrets;
- app shell/layout code;
- HTTP clients;
- business feature code.

## Runtime URLs

Shared assets are served at app-relative URLs:

```text
/assets/brand/tchalanet-logo.svg
/assets/i18n/fr/common.json
/assets/fonts/material-symbols-outlined.woff2
```

Apps must keep API URLs separate, for example `/api/v1/...`.

## SSR

`public-portal` SSR must not read `libs/shared-assets/public` directly at runtime. The Angular build copies shared assets into the browser output, and the Express server serves the browser output directory. This keeps SSR deployable as a built artifact without requiring workspace source folders in production.

## Ownership With i18n

`libs/shared-assets/public/assets/i18n` owns the local fallback JSON files. `libs/core/i18n` owns runtime loading, language state, bundle merge order, and backend/bootstrap overrides.
