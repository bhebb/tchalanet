# Change: minimal-public-admin-widget-bff

## Why

The backend already contains the main BFF/API routes for the weekly web delivery.
This change records the analysis and keeps backend work focused on contract confirmation,
targeted fixes, contract additions, and tests when the web integration exposes a real gap.

The work also sets backend-side rails for later integrations: PageModel/widget payloads,
role-resolved dashboards, action endpoints, and contract tests should become the default
pattern for future web surfaces.

## What changes

- Reuse public page widget payload: `GET /api/v1/public/page-models/{logicalId}`.
- Reuse platform dashboard payload: `GET /api/v1/platform/page-models`.
- Reuse tenant dashboard payload: `GET /api/v1/tenant/page-models`.
- Reuse tenant provisioning: `POST /api/v1/platform/tenant-onboarding/preview` and `/provision`.
- Reuse identity user creation for seller onboarding: `POST /api/v1/admin/identity/users`.
- Confirm response shapes are stable enough for the web slice.

## Impact

- Existing backend owners: `features/pagemodel`, `features/platformadmin`, and `platform/identity`.
- Web should consume existing APIs before requesting backend changes.
- Backend changes are in scope when needed for the approved public/superadmin/tenant-admin workflows.
- Future backend integrations should reuse the same contract style instead of adding ad hoc page-specific endpoints.

## Non-goals

- No cashier dashboard or cashier sale flow.
- No tenant analytics, sales summaries, draw summaries, payout, settlement, or reconciliation.
- No CMS or arbitrary page builder.
- No platform service health, jobs, feature flags, audit, or release notes.
- No database migration unless implementation proves one is required and human confirms it.
- No duplicate "create tenant" or "create seller" BFF endpoint when the existing endpoints support the workflow.
- No blanket backend freeze; targeted server edits are allowed for gaps, bugs, security, tenant-context correctness, widget payload completeness, and focused tests.
