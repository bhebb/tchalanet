# shared-assets

Canonical shared static assets for Tchalanet Web.

## Boundary

This lib owns files that every deployable portal can serve from `/assets/**`:

- brand logos and app icons;
- local i18n fallback JSON bundles;
- shared SVG icons and fonts;
- shared public media and markdown pages;
- path constants exported from `@tch/shared-assets`.

It does not own i18n runtime state, HTTP calls, shell layout, runtime secrets, or feature-specific
business code.

## Serving

Each app copies `libs/shared-assets/public` into its browser output. Public SSR serves assets from
the Angular browser output, not from the workspace source folder at runtime.
