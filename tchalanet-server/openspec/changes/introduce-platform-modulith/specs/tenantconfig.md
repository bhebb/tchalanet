# Spec — Migrate core.tenantconfig to platform.tenantconfig

## Goal

Move tenant configuration effective values and resolution to `platform.tenantconfig`.

## Old package

```text
com.tchalanet.server.core.tenantconfig.*
```

## New package

```text
com.tchalanet.server.platform.tenantconfig.api.*
com.tchalanet.server.platform.tenantconfig.internal.*
```

## Related catalog

```text
catalog.settings = setting definitions/default metadata
platform.tenantconfig = tenant-specific values, overrides, effective resolution
```

## Public API

```text
TenantConfigApi
TenantConfigView
TenantConfigValueView
ResolveTenantConfigRequest
UpdateTenantConfigRequest/Result if needed
```

## Internal implementation

```text
internal/app/TenantConfigResolver
internal/app/TenantConfigAdminService
internal/persistence/TenantConfigJpaEntity
internal/cache/TenantConfigCacheSpecs
internal/web/TenantConfigAdminController
```

## Migration tasks

- [ ] Create `TenantConfigApi.resolveEffective(TenantId)`.
- [ ] Move persistence to platform.
- [ ] Move admin endpoints to platform internal web.
- [ ] Ensure catalog settings are consumed via `catalog.settings.api` only.
- [ ] Update features/admin dashboards to call platform API.
- [ ] Replace core imports.
- [ ] Remove legacy package.

## Verification

- [ ] Effective config resolution unchanged.
- [ ] Cache invalidation after config writes works.
- [ ] No platform -> core dependency.
- [ ] No external internal import.
