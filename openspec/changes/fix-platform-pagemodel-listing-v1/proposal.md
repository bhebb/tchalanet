# Fix platform PageModel listing

## Problem

The platform PageModel screen calls `/admin/pagemodels`. That route resolves to the
`admin` API scope, while PostgreSQL only allows a super-admin to read PageModels
across tenants in the `platform` scope. The result is an empty list even when
PageModels exist.

The screen also has no tenant context for writes. After the list is fixed, editing,
publishing, resetting, or duplicating a model from another tenant must continue to
run with the existing tenant override contract so row-level security remains active.

## Proposal

- Expose the PageModel admin controller on the platform API surface while keeping
  the existing admin mapping for compatibility.
- Include the owning tenant ID in list summaries and apply the optional list
  filters in persistence.
- Update the platform web client to use `/platform/pagemodels`.
- Use the selected row's tenant ID with `asTenantAdmin` for detail and write
  requests; require a tenant ID when creating a new draft without a selected row.
- Add focused backend and web contract tests for the route and tenant context.

## Scope

This change fixes the platform PageModel management feature. It does not change
PageModel runtime resolution or tenant-admin dashboard rendering.
