# Platform Capability `platform.tenanttheme` — Tenant Theme

## Rôle

Appliquer et résoudre le thème actif d'un tenant à partir d'un preset `catalog.theme`.

**Ce module fait** :
- Appliquer un preset à un tenant (`applyTenantTheme(presetCode)`)
- Résoudre le thème effectif d'un tenant (`resolveTenantTheme`)
- Désactiver le thème d'un tenant
- Gérer la version et les métadonnées du thème actif

**Ce module ne fait pas** :
- Définition des presets globaux (→ `catalog.theme`)
- Rendu CSS ou application frontend
- Personnalisation de palette/tokens par tenant — un tenant choisit un preset, pas une couleur

---

## API — `TenantThemeApi`

```java
void            applyTenantTheme(ApplyTenantThemeRequest)
  // presetCode requis + non-blank — applique le preset au tenant

TenantThemeView resolveTenantTheme(ResolveTenantThemeRequest)
  // retourne le thème actif du tenant

void            deactivateTenantTheme(DeactivateTenantThemeRequest)
  // désactive le thème (tenant sans thème)
```

---

## Modèle — `TenantThemeView`

| Champ | Type | Sens |
|---|---|---|
| `tenantId` | `TenantId` | — |
| `presetCode` | `String` | Code du preset catalog appliqué |
| `metadata` | `Map<String,String>` | Métadonnées additionnelles |
| `isDefault` | `boolean` | Preset par défaut ? |
| `version` | `long` | Version du thème (incrémentée à chaque apply) |
| `updatedAt` | `Instant` | — |

---

## Invariants

- `presetCode` doit exister dans `catalog.theme` (actif) — validé à l'apply
- RLS actif
- Caching du thème résolu (eviction sur apply/deactivate)
- La version permet de détecter les changements côté frontend (ETag)

---

## Références

- Presets globaux : `catalog/theme/CATALOG_THEME.md`
- Provisioning : `tchalanet-docs/docs/02-functional/flows/tenant-onboarding.md`
