# Platform Tenant Provisioning Page V0

## Why

The current platform tenant provisioning route renders a placeholder while the older tenant create page already contains the provisioning form, preview, and submit logic. Superadmin needs one identifiable, serious provisioning flow that can become the pilot pattern for future complex admin forms.

## What

- Replace `PlatformTenantProvisioningPage` with a real superadmin provisioning form.
- Reuse `PlatformProvisioningApi.preview` and `PlatformProvisioningApi.provision`.
- Add reusable admin UI primitives for complex detail forms:
  - `AdminDetailLayoutComponent`
  - `TchIdentityCardComponent`
  - `AdminProvisioningHealthCardComponent`
- Keep `AdminPageShellComponent` as the page shell; do not add nested routing.
- Add local i18n fallback keys for the page.

## Current Inventory

- `platform/tenant-provisioning` and `platform/tenant-onboarding` route to the placeholder page.
- `platform/tenants/new` routes to `PlatformTenantCreatePage`, which has the usable form, preview, submit, snackbar, and result logic.
- Admin UI already has page shell, section card, empty state, and status pill primitives.
- Missing reusable detail layout, identity aside card, and provisioning health/preview card.

## Impact

- Web app only.
- No backend API changes.
- Route behavior changes from placeholder to a functional form.
- Existing tenant create page remains in place for now.

## Non-goals

- Tenant detail page.
- Tenant theme editor.
- Impersonation flow.
- Backend provisioning changes.
- Mobile/POS alignment.
