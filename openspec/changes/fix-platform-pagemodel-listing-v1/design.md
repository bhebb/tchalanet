# Design

## Request flow

```text
Platform portal
  GET /api/v1/platform/pagemodels
    -> api_scope=platform
    -> super-admin cross-tenant SELECT policy

Platform portal, selected tenant
  PUT/POST /api/v1/platform/pagemodels/{id}
    + X-Tch-Tenant-Override
    + X-Tch-Act-As: TENANT_ADMIN
    + X-Tch-Override-Reason
    -> effective tenant is bound for RLS writes
```

The platform route is an additional mapping on the existing controller. The old
`/admin/pagemodels` mapping remains available for compatibility, but the platform
portal no longer uses it for global listing.

List summaries expose `tenantId` so the UI can make the tenant context explicit.
Persistence builds a specification from the optional tenant, scope, and logical ID
filters rather than silently ignoring them.
