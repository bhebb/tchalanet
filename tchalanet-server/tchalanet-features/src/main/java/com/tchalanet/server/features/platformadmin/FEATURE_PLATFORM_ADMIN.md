# Feature: Platform Admin

## Scope

La feature `platformadmin` expose les endpoints réservés aux `SUPER_ADMIN` pour la gestion globale de la plateforme.

## Ce qui reste dans `features/platformadmin/`

### `overview/` — composite cross-catalog (feature légitime)

`PlatformAdminOverviewController` agrège plusieurs catalogs (settings, theme, i18n, tenants) en un seul payload composite. C'est une feature légitime car elle orchestre ≥ 2 catalogs.

## Ce qui a été migré vers `catalog/<bc>/internal/web/`

Les slices mono-catalog suivantes ont été supprimées et leurs endpoints absorbés par les controllers catalog :

| Ancienne slice features         | Controller catalog cible                                    | Endpoint principal                                                              |
| ------------------------------- | ----------------------------------------------------------- | ------------------------------------------------------------------------------- |
| `platformadmin/i18nglobal/`     | `catalog/i18n/internal/web/PlatformI18nOverridesController` | `GET /platform/i18n-overrides/overview`, `GET /platform/i18n-overrides/resolve` |
| `platformadmin/settingsglobal/` | `catalog/settings/internal/web/PlatformSettingsController`  | `GET /platform/settings/overview`                                               |
| `platformadmin/theme/`          | `catalog/theme/internal/web/ThemeAdminController`           | `GET /platform/theme-presets`, `GET /platform/theme-presets/overview`           |

## Règle

> CRUD mono-catalog → `catalog/<bc>/internal/web/`
> Feature légitime = BFF orchestrant ≥ 2 catalogs → `features/platformadmin/`
