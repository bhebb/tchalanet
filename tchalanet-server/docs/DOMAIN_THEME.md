# Domaine Theme (Catalog + Core)

> Documentation du domaine thème : gestion des presets globaux (`catalog/theme`) et application tenant (`core/tenanttheme`).

---

## 1. Rôle du domaine

**Responsabilité principale**

Fournir un catalogue de presets de thème UI réutilisables (couleurs, densité, mode) et gérer leur application au niveau tenant avec fallback automatique en cas d'indisponibilité.

**Ce que le domaine fait**

- Stocker et exposer des **Theme Presets** globaux (catalog/theme)
- Permettre l'activation/désactivation/retrait de presets via admin
- Appliquer un preset à un tenant (core/tenanttheme)
- Résoudre le thème effectif d'un tenant avec fallback automatique
- Émettre des warnings observables lorsqu'un preset est indisponible
- Garantir qu'aucun tenant ne reste sans thème fonctionnel

**Ce que le domaine ne fait pas**

- Rendu UI (responsabilité du frontend)
- Génération dynamique de thèmes (les presets sont pré-définis)
- Migration automatique de masse des tenants (opt-in seulement)
- Gestion des permissions utilisateurs (délégué à security)

---

## 2. Modèle métier (agrégats / entités)

### Catalog/theme (presets globaux)

- **`ThemePreset`** — Configuration réutilisable de thème UI (code, vendor, config JSON, active, deleted_at)
  - Identifié par : `ThemePresetId` (typed ID)
  - Clé fonctionnelle : `code` (unique, ex: "dark-v1", "modern-light")
  - États : `active` (publié/dépublié), `deleted_at` (retiré définitivement)

### Core/tenanttheme (application tenant)

- **`TenantTheme`** — Association tenant ↔ preset avec versioning
  - Identifié par : `TenantId` (composite key)
  - Référence : `presetCode` (string, pas de FK hard)
  - Métadonnées : surcharges personnalisées tenant (JSONB)
  - Version : incrémentée à chaque changement

### Invariants métier

- **DP1** : Un preset ne peut JAMAIS être hard-deleted (soft-delete only)
- **DP2** : Les `tenant_theme` ne sont JAMAIS modifiés automatiquement lors du retrait d'un preset
- **DP3** : La résolution du thème effectif DOIT toujours retourner un preset valide (via fallback)
- **T1** : L'application d'un preset valide le preset via `ThemeCatalog` avant persistance

> **Valeur métier clé** :  
> Garantir la stabilité UI des tenants même en cas de retrait/dépublication de presets, avec traçabilité complète des changements.

---

## 3. Cas d'utilisation (ports d'entrée)

### Catalog/theme (admin)

**Service interne** : `ThemePresetAdminService`

- `create(ThemePresetCreateRequest)` → `ThemePresetView`

  - Crée un nouveau preset global
  - Invalide les caches `ACTIVE_PRESETS` et `PRESET_BY_CODE`

- `update(ThemePresetId, ThemePresetUpdateRequest)` → `ThemePresetView`

  - Met à jour un preset existant
  - Invalide les caches

- `deactivate(ThemePresetId)` → `void`

  - Dépublication : set `active=false` (preset reste visible mais non actif)
  - Invalide les caches
  - **Note** : les tenants utilisant ce preset passeront en fallback

- `softDelete(ThemePresetId)` → `void`
  - Retrait définitif : set `deleted_at=now()` + `active=false`
  - Preset filtré de tous les reads
  - Invalide les caches

**Lecture publique** : `ThemeCatalog` (interface)

- `listActive()` → `List<ThemePresetView>`

  - Liste tous les presets actifs (`active=true`, `deleted_at IS NULL`)
  - Cachée : `ACTIVE_PRESETS`

- `findByCode(String)` → `Optional<ThemePresetView>`

  - Recherche par code fonctionnel
  - Retourne le preset même si `active=false` (tant que non soft-deleted)
  - Cachée : `PRESET_BY_CODE`

- `findById(ThemePresetId)` → `Optional<ThemePresetView>`
  - Recherche par ID technique (admin/internal)

### Core/tenanttheme (commands)

- `ApplyTenantThemeCommandHandler`

  - **Input** : `ApplyTenantThemeCommand(tenantId, presetCode)`
  - **Validation** : vérifie que le preset existe et est actif via `ThemeCatalog`
  - **Action** : persiste/update `TenantTheme` avec version++
  - **Event** : `TenantThemeUpdatedEvent` (après commit)

- `DeactivateTenantThemeCommandHandler`
  - **Input** : `DeactivateTenantThemeCommand(tenantId)`
  - **Action** : supprime/désactive le `TenantTheme`
  - **Event** : `TenantThemeUpdatedEvent`

### Core/tenanttheme (queries)

- `ResolveTenantThemeQueryHandler`
  - **Input** : `ResolveTenantThemeQuery(tenantId)`
  - **Output** : `TenantThemeView` (toujours non-null)
  - **Logique** :
    1. Lit `tenant_theme.preset_code`
    2. Vérifie disponibilité via `ThemeCatalog.findByCode(code)`
    3. Si indisponible (not found, inactive, ou soft-deleted) → **FALLBACK**
    4. Émet warning notice si fallback appliqué
    5. Retourne `TenantThemeView` avec preset effectif

---

## 4. Ports de sortie (dépendances externes)

### Catalog/theme

- `ThemePresetJpaRepository`
  - **Parle à** : table `theme_preset`
  - **Rôle** : CRUD sur les presets globaux
  - **Implémentation** : Spring Data JPA

### Core/tenanttheme

- `TenantThemePersistencePort` / `TenantThemeReaderPort`

  - **Parle à** : table `tenant_theme`
  - **Rôle** : CRUD tenant-scoped avec RLS
  - **Implémentation** : `TenantThemePersistenceAdapter` (JPA)

- `ThemeCatalog` (port out vers catalog/theme)
  - **Parle à** : `catalog/theme` via interface publique
  - **Rôle** : Valider presets et résoudre fallback
  - **Note** : Pas d'accès direct aux tables, utilise l'API publique du catalog

> **Principe de séparation** : `core/tenanttheme` ne touche JAMAIS la table `theme_preset` directement. Il passe toujours par `ThemeCatalog`.

---

## 5. Politique de retrait des presets (DP1-DP4)

### 5.1 Pas de hard delete (DP1)

Les presets ne peuvent JAMAIS être supprimés physiquement.

**Actions autorisées** :

- `deactivate()` → set `active=false` (dépublication temporaire)
- `softDelete()` → set `deleted_at!=NULL` + `active=false` (retrait définitif)

**Comportement des reads** :

- Tous les reads filtrent `deleted_at IS NULL`
- `listActive()` filtre en plus `active=true`
- `findByCode()` et `findById()` retournent les presets inactifs (mais pas soft-deleted)

**Rationale** :

- Préserve l'historique et la traçabilité
- Permet la réactivation d'un preset dépublié
- Évite les cascades destructrices sur `tenant_theme`

### 5.2 Pas de nettoyage automatique (DP2)

Lors du retrait/dépublication d'un preset, **aucune modification automatique** des `tenant_theme`.

**Ce qui NE SE PASSE PAS** :

- ❌ Cascade delete sur `tenant_theme`
- ❌ Reset automatique des tenants vers un autre preset
- ❌ Migration automatique de masse

**Rationale** :

- Préserve l'audit trail (on sait ce que le tenant avait configuré)
- Permet la réversibilité (réactiver le preset = tenants retrouvent leur config)
- Évite les surprises/risques opérationnels

### 5.3 Fallback automatique à la résolution (DP3)

Lorsqu'un tenant référence un preset indisponible, le système applique un **fallback transparent** à la volée (runtime, pas en base).

**Cascade de fallback** (ordre strict) :

1. **Tenant default** (si configuré via tenant registry/config)
2. **Platform default** (preset flaggé par défaut ou code conventionnel `default-light`)
3. **Hardcoded safe** (`default-light` en dur)

**Warning notice obligatoire** :

- Code : `THEME_PRESET_UNAVAILABLE_FALLBACK_APPLIED`
- Payload : `{ tenantId, requestedPresetCode, fallbackPresetCode, timestamp }`
- Émission :
  - Log structuré JSON avec marker `TENANT_THEME_FALLBACK` (niveau WARN)
  - Optionnel : event Spring pour listeners

**Observabilité** :

```json
{
  "level": "WARN",
  "marker": "TENANT_THEME_FALLBACK",
  "message": "Theme preset unavailable, fallback applied",
  "tenantId": "...",
  "requestedPresetCode": "old-preset",
  "fallbackPresetCode": "default-light",
  "timestamp": "2026-01-23T18:30:00Z"
}
```

### 5.4 Remédiation explicite opt-in (DP4)

La migration de masse des tenants (changer tous les tenants de preset X vers Y) est **hors scope MVP**.

Si implémentée ultérieurement, elle DOIT être :

- **Explicite** (action admin dédiée, pas implicite au retrait)
- **Opt-in** (l'admin décide quand/comment migrer)
- **Traçable** (logs + events)

---

## 6. Architecture technique

### 6.1 Catalog/theme (lecture cachée)

**Structure** :

```
catalog/theme/
  api/                          ← Interface publique
    ThemeCatalog.java           (interface read-only)
    ThemePresetView.java        (immutable DTO)
    ThemePresetId.java          (typed ID - stub deprecated, use common.types.id)
  internal/
    read/
      ThemePresetCatalogImpl.java  (implémente ThemeCatalog + cache)
    write/
      ThemePresetAdminService.java (admin CRUD)
    mapper/
      ThemePresetMapper.java       (MapStruct + CommonIdMapper)
    persistence/
      ThemePresetJpaEntity.java
      ThemePresetJpaRepository.java
    cache/
      ThemeCacheNames.java         (constantes)
    web/
      ThemeAdminController.java    (admin REST)
```

**Caching** :

- `ACTIVE_PRESETS` : cache `listActive()`
- `PRESET_BY_CODE` : cache `findByCode(code)`
- Invalidation : tous les writes (create/update/deactivate/softDelete)

### 6.2 Core/tenanttheme (CQRS + fallback)

**Structure** :

```
core/tenanttheme/
  domain/model/
    TenantTheme.java               (agrégat immutable)
  application/
    command/
      model/
        ApplyTenantThemeCommand.java
        DeactivateTenantThemeCommand.java
      handler/
        ApplyTenantThemeCommandHandler.java    (valide via ThemeCatalog)
        DeactivateTenantThemeCommandHandler.java
    query/
      model/
        ResolveTenantThemeQuery.java
        TenantThemeView.java
      handler/
        ResolveTenantThemeQueryHandler.java    (logique fallback)
    event/
      TenantThemeUpdatedEvent.java
    model/
      TenantThemeNotice.java                   (notice fallback)
    service/
      TenantThemeFallbackService.java          (cascade résolution)
    port/out/
      TenantThemePersistencePort.java
      TenantThemeReaderPort.java
  infra/
    persistence/
      TenantThemeJpaEntity.java                (RLS-ready)
      TenantThemeJpaRepository.java
      TenantThemePersistenceAdapter.java       (implémente les ports)
    web/
      TenantThemeController.java               (REST public)
```

**Transactions** :

- Commands : `@TchTx` (pas `@Transactional` Spring)
- Events : `AfterCommit.run(...)` pour garantir publication post-commit
- Queries : pas de tx (read-only)

**Typed IDs** :

- `TenantId`, `ThemePresetId` (jamais de `UUID` brut hors persistence)
- Converters Spring : `StringToThemePresetIdConverter` (délègue à `ThemePresetId.parse()`)

---

## 7. Règles métier importantes

### Règle 1 : Preset indisponible = fallback, jamais d'erreur

Un tenant DOIT toujours avoir un thème effectif, même si son preset configuré est retiré.

**Implémentation** : `ResolveTenantThemeQueryHandler` applique fallback automatique.

### Règle 2 : Validation à l'application, pas à la résolution

Lors de `ApplyTenantThemeCommand`, on valide que le preset existe ET est actif.  
Lors de `ResolveTenantThemeQuery`, on tolère l'inactif/retiré via fallback.

**Rationale** : éviter d'appliquer un preset déjà retiré, mais ne pas casser les tenants existants.

### Règle 3 : Les caches doivent être invalidés à chaque write

Tous les writes admin (create/update/deactivate/softDelete) DOIVENT invalider :

- `ACTIVE_PRESETS`
- `PRESET_BY_CODE`

**Rationale** : garantir cohérence lecture après écriture.

### Règle 4 : Tenant default configuration (TODO MVP)

La résolution du "tenant default" est prévue dans le design mais pas implémentée MVP.

**Placeholder** : `TenantThemeFallbackService` contient un TODO pour injecter `TenantConfigPort`.

---

## 8. Intégration avec les autres domaines

### Dépendances sortantes (core/tenanttheme → ...)

- **catalog/theme** (via `ThemeCatalog`)

  - Validation des presets lors de l'application
  - Résolution de fallback
  - **Couplage** : faible (interface publique)

- **core/tenant** (future)
  - Lire `tenant.default_theme_preset_code`
  - **État** : TODO (placeholder dans `TenantThemeFallbackService`)

### Dépendances entrantes (... → core/tenanttheme)

- **features/** (orchestration UI)

  - Appelle `ResolveTenantThemeQuery` pour obtenir le thème effectif d'un tenant
  - Utilise `TenantThemeView` pour rendre l'UI

- **Admin UI**
  - Appelle `ThemePresetAdminService` pour gérer les presets
  - Appelle `ApplyTenantThemeCommand` pour configurer un tenant

---

## 9. Tests et validation

### Tests unitaires (MUST)

- `TenantThemeFallbackServiceTest`
  - Scenarios : tenant default, platform default, hardcoded safe
- `ResolveTenantThemeQueryHandlerTest`
  - Scenarios : preset actif, inactive, soft-deleted, not found
  - Vérifier émission notice

### Tests d'intégration (SHOULD)

- Créer preset actif, l'appliquer à tenant, le désactiver, résoudre → vérifier fallback
- Vérifier que `tenant_theme` n'est pas modifié lors de la désactivation

### Validation manuelle (acceptance)

- Créer preset "test-preset", l'appliquer à tenant T
- Désactiver "test-preset"
- Appeler `GET /tenant/theme` (tenant T) → doit retourner fallback
- Vérifier logs : présence warning notice avec marker `TENANT_THEME_FALLBACK`

---

## 10. Performance et observabilité

### Performance (NF2)

- **Target** : fallback resolution < 50ms (warm cache)
- **Cascade** : max 3 lookups
- **Caches** : `ThemeCatalog` reads cachés (Caffeine/Spring Cache)

### Observabilité (NF1)

- **Logs structurés** : JSON avec marker `TENANT_THEME_FALLBACK`
- **Métriques** (optionnel) : `tenant_theme_fallback_total{requested_preset, fallback_preset}`
- **Audit** : `tenant_theme` non modifié = audit trail préservé

---

## 11. Références

- **Spec OpenSpec** : `openspec/specs/theme-preset/spec.md` (R1-R5)
- **Spec OpenSpec** : `openspec/specs/tenanttheme/spec.md` (T1-T6)
- **Change OpenSpec** : `openspec/changes/theme-preset-deletion-policy/` (DP1-DP4)
- **Conventions** :
  - `docs/conventions/typed_ids.md`
  - `docs/conventions/command_query_handlers.md`
  - `docs/conventions/cache.md`

---

## Changelog

- **2026-01-23** : Création initiale (phase 5 - theme-preset-deletion-policy)
  - Documentation politique retrait presets (DP1-DP4)
  - Documentation architecture catalog/theme + core/tenanttheme
  - Documentation cascade fallback et observabilité
