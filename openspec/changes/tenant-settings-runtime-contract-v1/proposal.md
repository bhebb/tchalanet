# Tenant Settings Runtime Contract V1

## Why

Tenant settings are stored as JSON and currently cross backend, admin web, platform web,
and mobile runtime bootstrap. The admin settings tab exposes a subset of the JSON, while
setup readiness treats the settings card as complete when a receipt-scoped display name differs
from the seed placeholder even though display name is canonical tenant/brand identity: it is
initialized from the tenant code at provisioning and can be changed by the tenant later.
This creates drift risk: obsolete UI fields, hidden required JSON fields, hardcoded language
or currency choices, and mobile/runtime behavior are not documented as one contract.

## What

- Define the canonical tenant settings JSON sections and identify deprecated or hidden fields.
- Keep tenant-admin write endpoints as BFF/orchestration in `features.tenantadmin`, with
  validation and persistence owned by `platform.tenant`.
- Align admin web settings edits with backend validation so saves preserve required hidden
  fields such as supported languages, fallback policy, and receipt template keys.
- Treat supported languages and currency values as backend-provided allowed-value policies used
  by admin/seller-terminal/send/print forms, not hardcoded client lists.
- Move tenant display name semantics to tenant/brand identity: provision from code, then allow
  tenant edits; receipt config may only keep a migration alias.
- Define setup readiness for the settings card as a backend-owned structural check, not a
  fragile client-only heuristic tied to a receipt-only display name.
- Plan mobile consumption through runtime bootstrap/state only; mobile must not edit tenant
  settings or duplicate backend business rules.

## Impact

- Backend: `platform.tenant` settings model, validation, persistence, runtime projection, and
  `features.tenantadmin` BFF endpoints.
- Web admin: `/app/admin/settings/config` and `/app/admin/setup` settings card.
- Web platform: tenant detail config summary.
- Mobile: runtime bootstrap consumption and settings documentation.

## Non-goals

- Replacing JSONB tenant settings with relational tables.
- Adding mobile tenant settings editing.
- Moving core business invariants into tenant config.
- Introducing Unleash or a new feature-flag provider.
