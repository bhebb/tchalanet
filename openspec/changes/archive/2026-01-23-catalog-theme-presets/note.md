# Theme Refactor — Catalog vs Core Split

> **Status**: APPROVED  
> **Scope**: `theme` domain refactor  
> **Goal**: Split the existing `theme` module into:
>
> - `catalog/theme` → **Theme presets (read-only, global)**
> - `core/tenanttheme` → **Tenant effective theme (apply, lifecycle)**

This document is the **authoritative move map** for refactoring the current `theme` package.

---

## 1. Context & Problem Statement

The current `theme` package mixes multiple responsibilities:

- global theme presets
- publication / archival lifecycle
- tenant application
- persistence, cache, events, web controllers

This violates the Catalog rules:

- `catalog` must be **reference data only**
- no lifecycle, no events, no workflows
- tenant-specific logic must live in `core`

👉 The module must be **split**.

---

## 2. Target Architecture (Final)

```
catalog/theme/ # READ-ONLY presets (global)
core/tenanttheme/ # Tenant effective theme (apply, lifecycle)
```

The existing `theme` module will be **fully removed** once migration is complete.

---

## 3. catalog/theme — Theme Presets (READ ONLY)

### Responsibility

- Store and expose **ThemePreset** definitions
- Global (non tenant-scoped)
- Read-mostly
- Cacheable
- No events, no lifecycle

### Final Shape

```
catalog/theme/
├─ api/
│ ├─ ThemeCatalog.java
│ ├─ ThemePresetId.java
│ └─ ThemePresetView.java
└─ internal/
  ├─ read/ThemeCatalogImpl.java
  ├─ write/ThemeAdminService.java
  ├─ mapper/ThemePresetMapper.java
  ├─ cache/ThemeCacheNames.java
  ├─ persistence/ThemePresetJpaEntity.java
  ├─ persistence/ThemePresetJpaRepository.java
  └─ web/ThemeAdminController.java
```

### What moves to `catalog/theme`

#### Persistence

FROM `theme/infra/persistence/ThemeJpaEntity`  
TO `catalog/theme/internal/persistence/ThemePresetJpaEntity`

- Remove `ThemeStatus`
- Keep only:
  - `code` (unique)
  - `preset_json` (jsonb)
  - `active`
  - `deleted_at`
- Must extend `BaseEntity` (global table)

#### Repository

FROM `theme/infra/persistence/JpaThemeRepository`  
TO `catalog/theme/internal/persistence/ThemePresetJpaRepository`

#### Queries → Catalog Read Adapter

REMOVE:

- ListThemesQueryHandler
- GetThemeByIdQueryHandler

Replaced by:

`catalog/theme/internal/read/ThemeCatalogImpl`

Methods:

- `listActive()`
- `findByCode(String code)`

#### Admin Controller

FROM `theme/infra/web/PlatformThemeController`  
TO `catalog/theme/internal/web/ThemeAdminController`

- Platform/Admin scope
- CRUD only
- No publish/archive

#### Cache

FROM `theme/infra/cache/ThemeCacheConfig`  
TO `catalog/theme/internal/cache/ThemeCacheNames`

- No `@Configuration`
- Cache via `@Cacheable` / `@CacheEvict`

---

## 4. core/tenanttheme — Tenant Effective Theme (APPLY)

### Responsibility

- Apply a theme to a tenant
- Manage lifecycle (apply, reset, update)
- Tenant-scoped persistence
- Versioning, audit
- Events after commit

### Final Shape

```
core/tenanttheme/
├─ domain/
│ ├─ model/TenantTheme.java
│ └─ exception/
├─ application/
│ ├─ command/
│ │ ├─ model/
│ │ └─ handler/
│ ├─ query/
│ └─ event/
├─ port/out/
└─ infra/
  ├─ persistence/
  ├─ cache/
  └─ web/
```

### What moves to `core/tenanttheme`

#### Commands (RENAMED)

FROM `theme/application/command/model/PublishThemeCommand`  
TO `core/tenanttheme/application/command/model/ApplyTenantThemeCommand`

FROM `theme/application/command/model/ArchiveThemeCommand`  
TO `core/tenanttheme/application/command/model/DeactivateTenantThemeCommand`

#### Command Handlers

FROM `theme/application/command/handler/PublishThemeCommandHandler`  
FROM `theme/application/command/handler/ArchiveThemeCommandHandler`  
TO `core/tenanttheme/application/command/handler/*`

- Validate preset via `ThemeCatalog`
- Tenant-scoped write
- Publish `TenantThemeUpdatedEvent` after commit

#### Domain Model (REPLACED)

REMOVE:

- `theme/domain/model/Theme`
- `theme/domain/model/ThemeStatus`

Replace with:

```java
TenantTheme {
  TenantId tenantId;
  String presetCode;
  ThemeMode mode;
  Density density;
  Map<String, String> overrides;
  long version;
}
```

#### Ports

FROM `theme/application/port/out/ThemeReaderPort`  
FROM `theme/application/port/out/ThemeWriterPort`  
TO `core/tenanttheme/port/out/*`

Ports are now tenant-scoped.

#### Persistence

FROM `theme/infra/persistence/ThemePersistenceAdapter`  
TO `core/tenanttheme/infra/persistence/TenantThemePersistenceAdapter`

Uses new table `tenant_theme` (RLS enabled).

---

## 5. Elements to REMOVE (No Replacement)

The following must be deleted:

- ThemeController
- ThemeRepositoryEventHandler
- automatic publish/archive hooks in persistence
- global cache config
- ThemeStatus enum
- CQRS query handlers for themes

---

## 6. Execution Order (Safe Refactor Plan)

1. Create `catalog/theme`
2. Move preset persistence + read API
3. Create `core/tenanttheme`
4. Move commands, handlers, domain
5. Add `tenant_theme` table
6. Update imports to use `ThemeCatalog`
7. Delete legacy `theme` module

---

## 7. Final Rules (Non-Negotiable)

**catalog/theme**

- presets only
- read-only
- no tenant logic
- no events

**core/tenanttheme**

- tenant-scoped
- lifecycle & apply
- events allowed

This split is authoritative and must be preserved by ArchUnit guards.

---

## 8. Next Steps (Optional)

- Add ArchUnit rules preventing `catalog.theme.internal` leakage
- Add Testcontainers tests for `tenant_theme`
- Wire `core/tenanttheme` into features/bootstrap

---

_End of document_
