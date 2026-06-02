# Change: platform-tenant-v1

## Decision

Tchalanet has one tenant lifecycle owner: `platform.tenant`.

`catalog.tenant` and `platform.tenantconfig` must not remain as two competing owners of the same `tenant` table.

`platform.tenant` owns tenant registry, lifecycle, base defaults, onboarding/provisioning, first tenant admin creation, runtime safe tenant views, readiness summary, and controlled pre-context tenant lookup.

## Why

The current model is confusing because `catalog.tenant` reads the `tenant` table through raw/RLS-bypass access while `platform.tenantconfig` writes/configures the same table.

A tenant is not a simple read-mostly catalog entry. It has lifecycle and operational behavior: creation, status, onboarding, provisioning, base defaults, and runtime resolution.

## What

Create/rename `platform.tenant` as the owner of the `tenant` table and tenant lifecycle.

Move or deprecate:

```text
catalog.tenant
platform.tenantconfig
```

in favor of:

```text
platform.tenant
```

`platform.tenant` includes a controlled read-only pre-context resolver because tenant resolution is needed before `TchRequestContext` and RLS binding are fully established.

## Impact

- tenant context resolution moves under `platform.tenant.internal.resolver`;
- public/private bootstrap can resolve safe tenant runtime information;
- tenant admin/platform admin use distinct views and endpoints;
- raw/RLS-bypass lookup is isolated and read-only;
- tenant admin screens must not use raw bypass flows.

## Non-goals

- Moving settings/i18n into `platform.tenant`.
- Owning games, theme tokens, permissions, entitlements, pricing, limits, promotions, terminals, outlets, or sessions.
- Creating a new tenant table.

## Success criteria

- The `tenant` table has exactly one lifecycle owner: `platform.tenant`.
- Pre-context lookup lives in `platform.tenant.internal.resolver` or is clearly deprecated from `catalog.tenant`.
- Tenant lifecycle writes no longer live in `platform.tenantconfig`.
- Public/private runtime view is safe and does not expose admin/internal config.
