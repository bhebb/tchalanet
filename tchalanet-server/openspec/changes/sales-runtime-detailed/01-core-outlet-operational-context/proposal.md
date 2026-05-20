# Change: core.outlet Operational Context

## Why

Outlet is not just an address/config row. In Tchalanet it controls operational sale ability:

- sales blocked / day closed
- opening float requirement
- auto-open/close session policy
- receipt configuration
- outlet-level limits/autonomy overrides
- assigned users and terminals

Therefore outlet remains a core domain with canonical admin/read endpoints.

## What

Implement `core.outlet` as source of truth for outlet CRUD and operational queries.

## Non-goals

- Do not put outlet CRUD in `features.tenantadmin`.
- Do not duplicate outlet mutations in feature layer.
