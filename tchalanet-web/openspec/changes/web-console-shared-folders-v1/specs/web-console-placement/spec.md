## ADDED Requirements

### Requirement: Console shared code has an owning library boundary

Reusable private-console code shared by admin and platform features SHALL live in
`libs/web/console` and be consumed through the `@tch/web/console` public API.

#### Scenario: Console feature needs a reusable helper or component

- **GIVEN** a helper, component, model, or data-access service is reused by multiple private
  console features
- **WHEN** it is moved out of an app-local `shared` folder
- **THEN** it lives under `libs/web/console/src/lib`
- **AND** consumers import it from `@tch/web/console`.

#### Scenario: Code is owned by one feature flow

- **GIVEN** a dialog, placeholder page, or orchestration helper is only used by one owning feature
  or one portal route tree
- **WHEN** the shared folders are audited
- **THEN** the code stays app-owned or moves to the owning feature boundary
- **AND** it is not promoted to `libs/web/console` only because it lived under a folder named
  `shared`.

### Requirement: Cross-surface assets and runtime config remain outside console

Static assets, translation/config payloads, runtime config, and feature flags SHALL remain in their
existing shared libraries when they are used by public and private surfaces.

#### Scenario: Code references asset URLs or runtime config

- **GIVEN** a console helper depends on `@tch/shared-assets` or `@tch/shared-config`
- **WHEN** the helper is moved to `@tch/web/console`
- **THEN** the asset/config ownership remains in `libs/shared-assets` or `libs/shared-config`
- **AND** no asset/config files are moved into `libs/web/console`.

#### Scenario: Public portal still needs shared assets

- **GIVEN** public portal or PageModel rendering uses shared assets/config
- **WHEN** console shared folders are migrated
- **THEN** public imports and runtime asset paths remain unchanged.
