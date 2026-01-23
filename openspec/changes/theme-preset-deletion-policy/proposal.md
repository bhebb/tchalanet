# Proposal: theme-preset-deletion-policy

**Change-id**: `theme-preset-deletion-policy`

## Résumé

Ce changement introduit une politique normative pour gérer les **TenantThemes** quand un **ThemePreset** est retiré (soft-delete ou `active=false`) dans `catalog/theme`.

**Objectif** : garantir que **aucun tenant n'est cassé**, tout en gardant la traçabilité et en rendant l'état "preset indisponible" visible.

---

## Motivation

- Un preset peut être dépublié ou retiré pour des raisons produit/design.
- Des tenants peuvent encore référencer ce preset via `tenant_theme.preset_code`.
- Sans politique explicite, on risque :
  - un bootstrap cassé
  - des tenants "sans thème"
  - des cascades destructrices non voulues

---

## Décision (normative)

### D1 — Aucun hard delete de ThemePreset

`catalog/theme` MUST NOT hard-delete presets.

- Actions autorisées :
  - `active=false` (dépublication)
  - `deleted_at != NULL` (retrait définitif)
- Reads filtrent toujours `deleted_at IS NULL`, et `listActive()` filtre `deleted_at IS NULL AND active=true`.

---

### D2 — Aucun nettoyage automatique des tenant themes

Lorsqu'un preset est retiré/dépublié, le système MUST NOT modifier automatiquement `tenant_theme`.

- Pas de cascade delete
- Pas de reset implicite
- Conservation à des fins d'audit / debug / réversibilité

---

### D3 — Fallback obligatoire à la résolution

`core/tenanttheme` MUST appliquer un fallback lors de la résolution du thème effectif si le preset référencé est indisponible :

**Indisponible** = (preset non trouvé) OR (preset soft-deleted) OR (preset inactive)

**Fallback recommandé** :

1. tenant default (si configuré)
2. platform default (preset marqué default)
3. hardcoded safe preset (dernier recours)

Le module MUST publier un warning (notice) lorsque le fallback est appliqué :

- Code: `THEME_PRESET_UNAVAILABLE_FALLBACK_APPLIED`
- Meta: `{ tenantId, requestedPresetCode, fallbackPresetCode }`

---

### D4 — Remédiation explicite (optionnel)

Le changement permet d'ajouter ultérieurement une remédiation explicite (hors scope MVP) :

- action admin "migrate preset X → Y"
- action admin "reset tenant themes using preset X"
- job batch de migration

Cette remédiation est **opt-in** et jamais implicite au moment du retrait du preset.

---

## Portée

- Mise à jour des specs :
  - `theme-preset spec` : interdiction hard delete + clarifier retrait/dépublication
  - `tenanttheme spec` : requirement "Resolve fallback + notice"
- Mise à jour de l'implémentation :
  - `ThemePresetAdminService` : supprimer hard delete, exposer `deactivate`/`softDelete`
  - `ResolveTenantThemeQueryHandler` : ajouter fallback + warning notice
- Tests :
  - cas preset inactive
  - cas preset soft-deleted
  - cas preset not found
  - vérification notice

---

## Non-objectifs

- Implémenter la remédiation de masse (migration de tenants) dans ce change.
- Modifier le modèle de données (pas de FK cascade).
- Ajouter un scheduler/batch de correction automatique.

---

## Critères d'acceptation

- La suppression "hard" n'existe pas dans `catalog/theme`.
- Un tenant référencant un preset retiré obtient un thème effectif via fallback.
- Un warning `THEME_PRESET_UNAVAILABLE_FALLBACK_APPLIED` est émis lors du fallback.
- Aucun `tenant_theme` n'est modifié automatiquement lors du retrait d'un preset.

---

## Validation

Après archivage du change, créer un spec delta :
`changes/theme-preset-deletion-policy/specs/theme-preset-deletion/spec.md`

Puis exécuter :

```bash
openspec validate theme-preset-deletion-policy --strict --no-interactive
```
