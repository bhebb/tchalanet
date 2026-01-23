# Tasks — theme-preset-deletion-policy

## Objectif

Implémenter la politique de gestion des presets retirés avec fallback automatique pour garantir qu'aucun tenant ne reste sans thème fonctionnel.

---

## Phase 1 — Spec updates

- [ ] 1.1 Mettre à jour `openspec/specs/theme-preset/spec.md`

  - Ajouter requirement interdisant hard delete
  - Clarifier différence entre dépublication (`active=false`) et retrait (`deleted_at != NULL`)
  - Documenter que reads filtrent toujours `deleted_at IS NULL`

- [ ] 1.2 Mettre à jour `openspec/specs/tenanttheme/spec.md`
  - Ajouter requirement T7 "Fallback resolution"
  - Spécifier cascade de fallback (tenant default → platform default → hardcoded safe)
  - Spécifier warning notice obligatoire lors du fallback

---

## Phase 2 — Catalog/theme (admin service)

- [ ] 2.1 Supprimer méthode `hardDelete` si elle existe dans `ThemePresetAdminService`
- [ ] 2.2 S'assurer que `softDelete(ThemePresetId)` fait bien :
  - `deleted_at = now()`
  - `active = false`
- [ ] 2.3 Ajouter méthode `deactivate(ThemePresetId)` (set `active=false` seulement)
- [ ] 2.4 Vérifier que `ThemeAdminController` n'expose pas de hard delete

---

## Phase 3 — Core/tenanttheme (fallback logic)

- [ ] 3.1 Créer `TenantThemeFallbackService` dans `core/tenanttheme/application/service`

  - Input: `TenantId`, `requestedPresetCode`
  - Output: `String fallbackPresetCode`
  - Logic: tenant default → platform default → hardcoded ("default-light")

- [ ] 3.2 Modifier `ResolveTenantThemeQueryHandler`

  - Appeler `ThemeCatalog.findByCode(presetCode)`
  - Si `Optional.empty()` OU `!active` → appeler fallback service
  - Émettre warning notice `THEME_PRESET_UNAVAILABLE_FALLBACK_APPLIED`
  - Retourner `TenantThemeView` avec le preset de fallback

- [ ] 3.3 Créer `TenantThemeNotice` (record)

  - `code: String` (ex: "THEME_PRESET_UNAVAILABLE_FALLBACK_APPLIED")
  - `tenantId: TenantId`
  - `requestedPresetCode: String`
  - `fallbackPresetCode: String`
  - `timestamp: Instant`

- [ ] 3.4 Intégrer notice publisher (log structuré ou event)
  - Utiliser logger avec marker ou publier via `ApplicationEventPublisher`

---

## Phase 4 — Tests

- [ ] 4.1 Tests unitaires `TenantThemeFallbackService`

  - Scenario: tenant default existe → retourne tenant default
  - Scenario: tenant default absent, platform default existe → retourne platform default
  - Scenario: aucun default → retourne hardcoded safe

- [ ] 4.2 Tests unitaires `ResolveTenantThemeQueryHandler`

  - Scenario: preset actif → retourne preset demandé, pas de notice
  - Scenario: preset inactive → retourne fallback + notice
  - Scenario: preset soft-deleted → retourne fallback + notice
  - Scenario: preset not found → retourne fallback + notice

- [ ] 4.3 Tests d'intégration (H2 ou Testcontainers)
  - Créer preset actif, l'appliquer à tenant, le désactiver, résoudre → vérifie fallback
  - Vérifier que `tenant_theme` n'est pas modifié lors de la désactivation du preset

---

## Phase 5 — Documentation

- [ ] 5.1 Ajouter section dans `DOMAIN_THEME.md` (ou créer si absent)

  - Expliquer politique de retrait
  - Expliquer cascade de fallback
  - Expliquer notice warning

- [ ] 5.2 Mettre à jour `README.md` ou `PLAYBOOK.md` si nécessaire

---

## Critères d'acceptation

- [ ] Hard delete n'existe plus dans `catalog/theme`
- [ ] `ResolveTenantThemeQuery` retourne toujours un thème valide (via fallback si nécessaire)
- [ ] Warning notice `THEME_PRESET_UNAVAILABLE_FALLBACK_APPLIED` est émis et observable (logs)
- [ ] Aucune modification automatique de `tenant_theme` lors du retrait d'un preset
- [ ] Tests couvrent les 4 cas (actif, inactive, soft-deleted, not found)

---

## Notes

- Remédiation de masse (migration batch) est hors scope de ce change
- FK cascade n'est pas ajoutée volontairement (on veut garder l'historique)
- Le fallback est transparent pour le tenant (pas de changement de `tenant_theme`)
