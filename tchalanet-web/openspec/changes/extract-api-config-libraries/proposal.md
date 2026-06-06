# Change: extract API and shared-config libraries

## Why

The active application still owns transverse HTTP contracts/helpers and runtime settings/feature
flag code. These boundaries are stable enough to extract and are prerequisites for later PageModel,
widgets, and Web-surface extraction.

## What changes

- Create `libs/api` with common backend/Web HTTP contracts, response helpers, error mapping, and
  common interceptors.
- Create `libs/shared-config` with runtime settings, settings API/mapping/store, feature flags, and
  their Angular directive/guard.
- Update the active app to consume the new public library APIs.
- Remove the migrated sources from the app and their obsolete backup counterparts when applicable.
- Document the next extraction boundaries for PageModel, widgets, dashboards, and Web surfaces.

## Impact

- Touches only `tchalanet-web`.
- Adds two Nx libraries backed by existing active code.
- Changes imports without changing runtime API behavior.

## Non-goals

- No PageModel, widgets, auth, i18n, dashboard, or route extraction in this change.
- No new facade layer.
- No migration of legacy backup API/config implementations that do not match active contracts.
