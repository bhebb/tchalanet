# CATALOG_THEME

> Référentiel global des presets de thème visuels.  
> Un preset = une configuration de thème prête à l'emploi (couleurs, mode, vendor).  
> Catalog global — pas de tenant-scope, pas d'états lifecycle, pas d'événements.

---

## Rôle

`catalog.theme` répond à : **quels presets de thème sont disponibles sur la plateforme ?**

Un preset est une configuration de thème globale (définie par la plateforme ou un vendor). Les tenants y font référence via `platform.tenanttheme` qui contient la personnalisation spécifique au tenant (palette, tokens, cssVars, cycle DRAFT/PUBLISHED/ARCHIVED).

`catalog.theme` ≠ `platform.tenanttheme` — catalog = référentiel global de presets, platform = thème personnalisé du tenant.

---

## Modèle — `ThemePresetView`

| Champ | Type | Sémantique |
|---|---|---|
| `id` | `ThemePresetId` | Identifiant |
| `code` | `String` | Code stable, ex: `default-light`, `tchalanet-dark` |
| `vendor` | `String` | Fournisseur du preset, ex: `tchalanet`, `custom` |
| `config` | `JsonNode` | Configuration complète du preset (couleurs, tokens, variables CSS) |
| `labelKey` | `String` | Clé i18n du nom du preset |
| `isDefault` | `boolean` | Preset par défaut proposé aux nouveaux tenants |
| `active` | `boolean` | Preset actif (visible en admin) |
| `createdAt` | `Instant` | — |
| `updatedAt` | `Instant` | — |

### `ThemeMode` — enum (utilisé dans les configs de preset)

| Valeur | Sens |
|---|---|
| `LIGHT` | Mode clair |
| `DARK` | Mode sombre |
| `SYSTEM` | Suit les préférences système de l'utilisateur |

---

## API publique — `ThemeCatalog`

```java
List<ThemePresetView> listActive()
Optional<ThemePresetView> findById(ThemePresetId id)
Optional<ThemePresetView> findByCode(String code)
ThemePresetStatsView stats()    // total + active count
```

---

## Invariants

- `code` unique — identifiant stable du preset
- `isDefault = true` : au plus un preset par défaut actif (non enforced par DB — convention)
- `config` : blob JSON opaque pour ce catalog — interprété par `platform.tenanttheme` et le frontend

---

## Séparation des responsabilités

| `catalog.theme` | `platform.tenanttheme` |
|---|---|
| Presets globaux définis par la plateforme | Thème personnalisé d'un tenant |
| Global (pas de tenantId) | Tenant-scoped (RLS) |
| Pas de lifecycle (actif/inactif seulement) | Cycle DRAFT → PUBLISHED → ARCHIVED |
| Pas d'événements | Publie des événements de publication |
| Référentiel de templates | Personnalisation effective |

Lors du provisioning d'un tenant : `platform.tenanttheme` duplique un preset `catalog.theme` comme point de départ.

---

## Références

- Provisioning : `tchalanet-docs/docs/02-functional/flows/tenant-onboarding.md`
- Thème tenant : `tchalanet-platform/.../tenanttheme/PLATFORM_TENANTTHEME.md`
