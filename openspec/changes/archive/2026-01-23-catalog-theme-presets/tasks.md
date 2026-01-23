# Tasks - catalog-theme-presets

But : livrer un catalogue `catalog/theme` read-only et préparer `core/tenanttheme` pour le lifecycle tenant.

## Tâches détaillées (ordre recommandé)

Phase 0 — Préparations

- [x] Créer une issue/PR décrivant le plan de migration (référence `note.md`).
- [ ] Ajouter une entrée dans CHANGELOG et tracker de release.

Phase 1 — Catalogue (catalog/theme)

1. Scaffolding

   - [x] Créer l'arborescence : `catalog/theme/api`, `catalog/theme/internal/{read,write,mapper,persistence,cache,web}`.
   - [ ] Ajouter le fichier de migration SQL `V__create_theme_preset_table.sql` (table `theme_preset`).

2. Modèle & API

   - [x] `common/types/id/ThemePresetId.java` (typed id)
   - [x] ajoute la converstion dans le mapper `common/mapper/CommonIdMapper.java`
   - [x] `catalog/theme/api/ThemePresetView.java` (view immuable: id, code, vendor?, config(JsonNode), labelKey?, active, createdAt, updatedAt)
   - [x] `catalog/theme/api/ThemePresetCatalog.java` (interface: listActive, findById, findByCode)

3. Persistence & Mapper

   - [x] `catalog/theme/internal/persistence/ThemePresetJpaEntity.java` (fields + extend BaseEntity)
   - [x] `catalog/theme/internal/persistence/ThemePresetJpaRepository.java`
   - [x] `catalog/theme/internal/mapper/ThemePresetMapper.java` en utilisant CommonIdMapper(MapStruct)

4. Read implementation

   - [x] `catalog/theme/internal/read/ThemePresetCatalogImpl.java` (uses repo, mapper; @Cacheable; filter deleted_at)
   - [x] Définir `catalog/theme/internal/cache/ThemeCacheNames.java` respecte le pattern `catalog.theme.cache.*`

5. Admin write (internal)

   - [x] `catalog/theme/internal/write/ThemePresetAdminService.java` (create/update/softDelete) — retourne `ThemePresetView` via mapper
   - [x] `catalog/theme/internal/web/ThemeAdminController.java` (protégé) — endpoints CRUD admin en se referant a conventions/api/\*
   - [x] `@CacheEvict` sur writes pour `ACTIVE` et `BY_CODE`

6. Tests & qualité

   - [x] Tests unitaires pour `ThemePresetMapper`.
   - [x] Tests d'intégration H2 pour `ThemePresetCatalogImpl` (list/findById/findByCode, filtre deleted_at).
   - [ ] Tests de sécurité pour controller admin (permissions).
   - [x] Ajouter ArchUnit tests :
     - [x] `catalog.*.api` ne doit pas dépendre de `catalog.*.internal`
     - [ ] `catalog.*.internal.*.web` ne doit pas exposer JPA entities

7. Documentation
   - [ ] `DOMAIN_THEME.md` (résumé du domaine et du split)
   - [ ] Mettre à jour docs d'architecture et README de module

Phase 2 — Core tenanttheme (spec ajouté)

1. Scaffolding core/tenanttheme

   - [ ] Créer l'arborescence `core/tenanttheme` (domain, application, infra, ports)

2. Domain & Commands

   - [ ] `ApplyTenantThemeCommand`, `DeactivateTenantThemeCommand` et handlers
   - [ ] Command handlers doivent valider le preset via `ThemePresetCatalog` (read API)

3. Persistence & events

   - [ ] `tenant_theme` persistence adapter (RLS ready)
   - [ ] Publier `TenantThemeUpdatedEvent` après commit

4. Tests
   - [ ] Unit/Integration tests pour handlers

Phase 3 — cleanup

- [ ] Retirer module legacy `theme/` une fois tout validé
- [ ] Nettoyer stubs/redirects et supprimer code obsolète

## Critères d'acceptation & vérification

- [ ] `listActive()` renvoie uniquement presets actifs et non supprimés
- [ ] Insertion d'un preset avec `code` dupliqué échoue (contraintes DB) et service renvoie erreur lisible
- [ ] Cache invalidation testée après writes
- [x] ArchUnit rules appliquées et tests verts
- [ ] Documentation ajoutée

## Notes opérationnelles

- Préférer contrainte DB UNIQUE(code) (possiblement partial index WHERE deleted_at IS NULL) pour sécurité forte.
- Les API publiques et DTO doivent rester stables; privilégier retour de Views immuables.
- Les tests ArchUnit doivent être ajoutés tôt pour empêcher regressions.

---
