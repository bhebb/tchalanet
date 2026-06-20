# Platform Tenants V0

## Why

Super Admin needs a usable tenant management surface for listing tenants and provisioning a new tenant with an operational baseline.

## What

- Complete `/app/platform/tenants` with query-backed filters, loading/error/empty/ready states, V0 columns, and detail navigation.
- Complete `/app/platform/tenants/new` as a tenant provisioning flow using existing preview/provision endpoints.
- Add a minimal tenant detail page for post-provision confirmation and direct list navigation.
- Keep the existing backend provisioning endpoints intact.

## Impact

- Web platform tenant pages and services.
- Local i18n bundles.
- Focused frontend tests where practical.

## Non-goals

- Full tenant detail editor.
- Full billing, entitlements, or admin identity editor.
- Backend package rename or new alias endpoints.
