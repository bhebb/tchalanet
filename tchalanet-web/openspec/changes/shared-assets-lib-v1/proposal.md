# shared-assets-lib-v1

## Why

Tchalanet Web has several deployable apps that need the same brand assets, local i18n fallback bundles, icons, fonts, and public static media. Today those assets are owned by `apps/tch-portal/public/assets`, which makes the legacy app the implicit source of truth and forces new apps to duplicate or reference an app folder.

The multi-portal structure needs a shared, app-independent asset source that still works with Angular browser builds and public SSR.

## What

- Create `libs/shared-assets` as the canonical Nx lib for shared static web assets.
- Move shared runtime assets under `libs/shared-assets/public/assets`.
- Export typed path helpers/constants from `@tch/shared-assets` for common brand, i18n, font, social, public, fallback, and config asset URLs.
- Configure `tch-portal`, `public-portal`, `admin-portal`, and `platform-portal` to copy shared assets into each app browser output at `/assets/**`.
- Keep app-specific static files such as each app `favicon.ico` in the app `public` folder.
- Document the boundary and SSR serving rule: public SSR serves the Angular browser output, so shared assets must be copied by the Angular asset pipeline rather than read from the filesystem at runtime.

## Impact

- Apps can build and deploy independently while using the same `/assets/**` URLs.
- `core/i18n` keeps owning runtime language loading/merge behavior, while `shared-assets` owns the physical fallback JSON files.
- UI/web libs can reference stable asset constants without depending on a specific app.

## Non-goals

- No runtime config secrets in shared assets.
- No CDN, upload, or media management system.
- No split into many asset libs.
- No broad rewrite of every feature image reference in this slice.
