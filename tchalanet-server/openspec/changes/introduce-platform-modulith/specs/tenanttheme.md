# Spec — Migrate core.tenanttheme to platform.tenanttheme

## Goal

Move tenant theme overrides/effective theme resolution to `platform.tenanttheme`.

## Old package

```text
com.tchalanet.server.core.tenanttheme.*
```

## New package

```text
com.tchalanet.server.platform.tenanttheme.api.*
com.tchalanet.server.platform.tenanttheme.internal.*
```

## Related catalog

```text
catalog.theme = global theme presets
platform.tenanttheme = tenant overrides and effective runtime theme
```

## Public API

```text
TenantThemeApi
TenantThemeView
ResolvedTenantThemeView
UpdateTenantThemeRequest/Result
```

## Internal implementation

```text
internal/app/TenantThemeResolver
internal/app/TenantThemeAdminService
internal/persistence/TenantThemeJpaEntity
internal/web/TenantThemeAdminController
internal/cache/TenantThemeCacheSpecs
```

## Rename rules

```text
TenantThemeService -> TenantThemeResolver or TenantThemeAdminService depending on responsibility
Theme override entities -> TenantThemeJpaEntity under platform
```

## Migration tasks

- [ ] Create `TenantThemeApi`.
- [ ] Move tenant override persistence.
- [ ] Consume theme presets via `catalog.theme.api`.
- [ ] Move admin web endpoints.
- [ ] Update public/page model/theme consumers.
- [ ] Remove legacy package.

## Verification

- [ ] Effective theme fallback still works.
- [ ] Tenant override writes evict caches.
- [ ] No catalog -> platform dependency.
- [ ] No platform -> core dependency.
