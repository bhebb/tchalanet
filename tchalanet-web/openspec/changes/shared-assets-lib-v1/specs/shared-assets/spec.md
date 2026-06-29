## ADDED Requirements

### Requirement: Shared static asset ownership

Shared browser assets MUST be physically owned by `libs/shared-assets/public/assets` instead of by a deployable app folder.

#### Scenario: A shared logo is referenced by multiple apps

- **WHEN** a logo is needed by `public-portal`, `admin-portal`, `platform-portal`, or `tch-portal`
- **THEN** the source file lives under `libs/shared-assets/public/assets/brand`
- **AND** app-specific `public` folders do not duplicate the shared logo file.

### Requirement: Per-app asset copying

Each deployable app MUST configure its Angular build to copy `libs/shared-assets/public` into its browser output.

#### Scenario: A portal app builds independently

- **WHEN** `public-portal`, `admin-portal`, `platform-portal`, or `tch-portal` is built
- **THEN** shared files from `libs/shared-assets/public/assets` are emitted under `/assets/**`
- **AND** app-specific files such as `favicon.ico` may remain in `apps/<app>/public`.

### Requirement: SSR asset serving

Public SSR MUST serve shared assets from the generated browser output, not from the workspace source folder.

#### Scenario: Public portal SSR handles an asset request

- **WHEN** `public-portal` is deployed as an SSR build
- **THEN** `/assets/**` resolves from the built browser output
- **AND** runtime server code does not read `libs/shared-assets/public` directly.

### Requirement: Stable asset URL helpers

Shared TypeScript code MUST reference common asset URLs through `@tch/shared-assets` constants or helpers.

#### Scenario: A shared shell needs a brand or social icon URL

- **WHEN** shell or runtime code needs a common brand, social, i18n, fallback, font, public, or lottery asset URL
- **THEN** it can import a stable path constant from `@tch/shared-assets`
- **AND** the helper returns app-relative `/assets/**` URLs without reading `window` or `document`.
