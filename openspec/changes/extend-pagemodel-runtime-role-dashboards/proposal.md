# Change: Extend PageModel Runtime and Role Dashboards

## Status

Proposed

## Summary

Tchalanet already has the main PageModel building blocks:

- `catalog.pagetemplate` stores default/system templates.
- `core.pagemodel` resolves effective PageModels.
- `features.pagemodel` exposes runtime BFF endpoints and enriches dynamic widgets through providers.

This change completes the runtime path for:

- public home page;
- cashier dashboard;
- tenant admin dashboard;
- platform super admin dashboard;
- SUPER_ADMIN tenant override mode, where a super admin sees the same tenant dashboard as a tenant admin.

The PageModel/template administration UI and merge/replace admin flows are intentionally not prioritized in this change.

## Why

The frontend needs stable PageModel JSON for public and private pages. The current backend structure already has a dynamic provider mechanism, but several providers, system templates, and runtime resolution rules are missing or ambiguous.

The most important ambiguity is dashboard selection. Role alone is insufficient because SUPER_ADMIN has two modes:

1. Platform mode: sees `platform.dashboard.super_admin`.
2. Tenant override mode: sees `private.dashboard.tenant_admin` for the selected tenant.

Therefore dashboard resolution must use `TchRequestContext` with scope, role, effective tenant, and tenant override state.

## What changes

This change will:

- normalize dynamic `binding.source` names across public, cashier, tenant admin, and platform templates;
- complete system JSON templates for:
  - `public.home`;
  - `private.dashboard.cashier`;
  - `private.dashboard.tenant_admin`;
  - `platform.dashboard.super_admin`;
- add missing dynamic providers for public, cashier, tenant admin, and platform dashboards;
- refactor `PageModelTypeResolver` to resolve dashboards from `TchRequestContext`, not `TchRole` alone;
- split `DashboardPageModelService` runtime methods between tenant and platform modes;
- keep provider failures isolated per widget;
- return dynamic payloads under `dynamic.widgets[widgetId]` and widget errors under `dynamic.errors`;
- preserve current architecture where features orchestrate BFF composition and do not own PageModel persistence or business rules.

## Out of scope

- PageModel admin UI.
- PageTemplate admin UI.
- Template merge/replace admin workflows.
- Full operator dashboard implementation, unless added later as a small extension.
- Frontend widget component implementation.
- New persistence model for PageModel.
- Moving business rules into feature providers.
- Money, payout, settlement, or limit calculations inside BFF providers.

## Ownership rules

- `catalog.pagetemplate` owns default/system declarative templates.
- `core.pagemodel` owns effective PageModel resolution, publication state, tenant override resolution, and model source of truth.
- `features.pagemodel` owns runtime BFF composition and dynamic provider orchestration.
- Dynamic providers may call `QueryBus`, `CommandBus` only when appropriate for read orchestration, and catalog APIs.
- Dynamic providers must not access repositories, JPA entities, or persistence adapters.
- Dynamic providers must not implement business invariants.

## Affected areas

### Backend packages

- `com.tchalanet.server.features.pagemodel.dashboard`
- `com.tchalanet.server.features.pagemodel.dynamic`
- `com.tchalanet.server.features.pagemodel.dynamic.providers`
- PageTemplate/PageModel seed JSON resources

### Runtime endpoints

Existing runtime endpoints remain the primary entrypoints:

- `GET /tenant/pagemodel/dashboard`
- `GET /platform/pagemodel/dashboard`

Preview/logical-id endpoints may remain if they already exist, but runtime tenant users should not be able to freely choose arbitrary private/platform logical IDs.

## Risks

- Provider failures could break the full dashboard if not isolated.
- SUPER_ADMIN dashboard resolution can become ambiguous without scope-aware rules.
- Tenant override mode could hide the fact that a platform user is acting in a tenant context.
- Providers could accidentally expose internal exception messages.
- Providers could drift into business logic if not reviewed.

## Mitigations

- Provider errors are captured per widget and returned as sanitized `WidgetDynamicError` values.
- Internal exceptions are logged server-side only.
- Dashboard resolution uses `TchRequestContext` with `apiScope`, `currentRole`, `tenantOverridden`, and effective tenant.
- SUPER_ADMIN tenant override resolves to tenant admin dashboard only in TENANT scope.
- UI context must expose enough metadata for an override banner.
- Providers must use QueryBus/catalog APIs and stay read-only/orchestration-only.
