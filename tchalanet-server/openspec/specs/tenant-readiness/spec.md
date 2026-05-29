# tenant-readiness Specification

## Purpose
TBD - created by archiving change dashboard-overview-runtime-v1. Update Purpose after archive.
## Requirements
### Requirement: Tenant readiness has one calculation and multiple projections

Tenant readiness SHALL be computed once and projected differently.

| Projection | Consumer | Content |
|---|---|---|
| `TenantReadinessSummary` | dashboard | short status, missing count, top issues |
| `TenantReadinessView` | tenant overview | sections, status, issues, routes |
| `TenantReadinessView` | provisioning result | readiness and next steps |

### Requirement: Readiness is structural

Readiness SHALL check structural completeness such as:

- tenant identity complete;
- tenant admin exists;
- at least one active outlet;
- at least one active seller;
- at least one active/bindable terminal;
- games enabled;
- pricing coverage;
- draw channels configured;
- PageModels present;
- settings defaults present;
- minimum i18n present;
- theme present;
- limit policy templates or active policies according to rule;
- promotion templates optional.

### Requirement: Readiness excludes dashboard activity KPIs

Readiness SHALL NOT include:

- salesToday;
- ticketCountToday;
- activeSessions as real-time KPI;
- openDraws as dashboard KPI;
- unread notifications.

### Requirement: Readiness uses context, not client tenant id

For tenant/admin scope, readiness SHALL use effective tenant from current context/RLS.

#### Scenario: Tenant admin requests readiness-backed overview

- **WHEN** the tenant admin opens `/admin/overview`
- **THEN** readiness uses the current effective tenant
- **AND** no tenant id from the client body is trusted.

