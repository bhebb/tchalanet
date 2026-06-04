# Change: week-public-admin-minimal-dashboards

## Why

The current PageModel/dashboard plan is too broad for this week's delivery.
It mixes public widgets, cashier dashboard, tenant admin analytics, platform operations,
feature flags, jobs, audit, and release notes.

This change narrows the cross-project scope to three deliverables:

1. a public page rendered by a widget engine;
2. a minimal SUPER_ADMIN dashboard for creating tenants and tenant admins;
3. a minimal TENANT_ADMIN dashboard for onboarding sellers.

It also lays the foundation for later integrations by standardizing how web surfaces consume
backend PageModel/widget payloads and action endpoints. Those future integrations are enabled
by the structure, but remain outside this slice.

## What changes

- Coordinate backend and web OpenSpec changes for the same minimal product slice.
- Confirm and document the backend contracts already present for the web runtime.
- Define the web screens and widget rendering behavior needed for the first usable UI.
- Keep dashboard capabilities intentionally small and action-oriented.

## Impact

- Backend change: `tchalanet-server/openspec/changes/minimal-public-admin-widget-bff`.
- Web change: `tchalanet-web/openspec/changes/minimal-public-admin-widget-ui`.
- Existing broad PageModel/dashboard changes may be superseded or split later, but this change does not archive them.
- Later integrations should build on the same widget rendering, role routing, typed API service, and backend-contract validation patterns.

## Non-goals

- No cashier dashboard in this weekly slice.
- No analytics dashboards.
- No full tenant management console.
- No CMS/page builder.
- No platform audit, jobs, flags, service-health, or release-note widgets.
- No seller management beyond initial onboarding.
- Backend changes are allowed when the existing implementation has a real integration gap, bug, incomplete contract, missing authorization check, or missing test coverage for this slice.
