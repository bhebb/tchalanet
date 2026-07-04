## Why

Tenant onboarding and tenant admin configuration need a clean split between tenant plan entitlements and user permissions. Current plan seeds still reference removed slices such as outlets, cashier sessions, and generic terminals, while the runtime model now uses seller terminals and tenant admins.

## What Changes

- Align plan limits around admin users, seller terminals, draw channels, and promotion rules.
- Keep permissions as RBAC grants only; entitlements remain tenant-plan capabilities and quotas.
- Ensure DEMO tenants receive broad entitlements and high quotas for onboarding validation.
- Remove the CASHIER system role seed and local cashier user seed.
- Remove the HT_NUMERO game from seed data and provisioning defaults.
- Enforce quotas on tenant admin creation and seller terminal creation.

## Non-Goals

- No new database migration files; the existing seed files are edited directly because the target database has not been created yet.
- No tenant-admin draw-channel creation endpoint change in this slice.
- No frontend implementation in this slice.
