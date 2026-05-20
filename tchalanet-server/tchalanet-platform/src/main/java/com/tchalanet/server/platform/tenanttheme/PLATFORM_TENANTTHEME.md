# Platform Capability `platform.tenanttheme` — Tenant Theme Management

> Archetype : Application Service Module. Migré depuis `core.tenanttheme`.

## 1. Rôle

Gérer les overrides de thème (couleurs, logos, polices) par tenant, au-dessus des presets définis dans `catalog.theme`.

**Ce module fait** :
- Stocker et exposer les overrides de thème par tenant.
- Fournir le thème effectif = preset catalog + overrides tenant.
- CRUD admin des overrides.

**Ce module ne fait pas** :
- Définition des presets de thème (→ `catalog.theme`).
- Rendu CSS/frontend (→ web app).

## 2. Structure

```text
platform/tenanttheme/
  api/
    TenantThemeApi.java       ← getEffectiveTheme(TenantId) → TenantThemeView
    model/
      TenantThemeView.java
      ThemeOverride.java
  internal/
    service/                  ← merge preset + overrides
    persistence/
    web/                      ← ThemeAdminController (/api/v1/admin/theme)
    cache/
    config/
```

## 3. Règles

- Consomme `catalog.theme.api` pour lire les presets.
- RLS actif sur les overrides.
- Caching du thème effectif (TTL, evict sur update admin).
