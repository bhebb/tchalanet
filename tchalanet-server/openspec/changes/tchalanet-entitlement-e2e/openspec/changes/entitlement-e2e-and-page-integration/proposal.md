# OpenSpec Change — Entitlement E2E Tests & Page Integration

## Status

Draft / Ready for implementation after `platform.entitlement` compiles.

## Goal

Stabilize the new entitlement capability before mapping it everywhere.

This change frames:

1. Python E2E tests for entitlement.
2. Integration into the existing multitenant/onboarding test flows.
3. Follow-up page generation changes once entitlement is validated.
4. Public plan display.
5. Dashboard usage counts.
6. Progressive service-to-entitlement mapping.
7. A small architecture rule: before developing a new API or handler, explicitly decide whether entitlement applies.

## Non-goals

This change does not implement:

- Theme builder.
- Custom role builder.
- Visual rule editor.
- Billing automation.
- Enterprise marketplace.
- External public API access.

## Decisions

- `catalog.plan` remains the source of plan definitions: features JSON + limits JSON.
- `core.subscription` remains the owner of tenant subscription lifecycle.
- `platform.entitlement` resolves runtime capabilities through `TenantCapabilitySnapshot`.
- Python E2E tests must validate the entitlement foundation before business modules are mapped broadly.
- Public page generation can display plans, but the backend remains the source of truth for capabilities.
- Dashboards may display usage counts, but quota enforcement belongs to entitlement + domain handlers.

## Success Criteria

- E2E tests prove plan → subscription → capabilities works across tenants.
- Subscription changes invalidate entitlement cache.
- Quota checks block users/outlets/terminals when limits are reached.
- Feature gates block unavailable features.
- Onboarding creates tenant subscriptions correctly.
- Public pages can list active plans and selected plan features.
- Dashboard payloads include usage counts for relevant limits.
- New API/handler review checklist includes entitlement decision.
