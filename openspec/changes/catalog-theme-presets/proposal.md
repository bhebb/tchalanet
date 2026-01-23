# Proposal: catalog-theme-presets

Change-id: catalog-theme-presets

## Résumé

Ce changement organise la séparation du domaine `theme` en deux sous-systèmes clairement distincts :

- `catalog/theme` — stockage et exposition de ThemePreset stables (préconfigurations globales, read‑only) ;
- `core/tenanttheme` — gestion du cycle de vie tenant du theme (apply, activation, historique, events).

L'objectif est d'appliquer la règle d'architecture `75-catalog-rules.md`: les catalogues sont des données de référence, sans logique de lifecycle ni opérations d'apply.

## Motivation

- Éviter le mélange de responsabilités (presets globaux vs lifecycle tenant) dans le même module.
- Fournir un contrat de lecture stable et cacheable pour les consommateurs (UI, services core).
- Dégager un emplacement clair pour la logique tenant (versionning, apply, événements) sous `core/tenanttheme`.

## Ce que fait ce change

- Spécifie et scinde la responsabilité : création d'un catalogue `catalog/theme` en lecture seule et d'un nouveau module `core/tenanttheme` pour le lifecycle.
- Définit les artefacts à créer : API, entité JPA, repository, mapper, impl. read cacheable, service admin interne (write), controller admin protégé, scripts de migration SQL, tests et règles ArchUnit.
- Fournit un plan de migration progressif et sûr (déployer catalogue en parallèle, créer core/tenanttheme, rediriger handlers, supprimer le legacy).

## Ce que ce change NE fait PAS

- Il n'implémente aucune logique d'"apply" ou d'activation pour un tenant dans le catalogue. Toute action d'activation appartient à `core/tenanttheme`.
- Il ne gère pas la provisioning tenant/catalog — ces liens seront faits par `core/tenanttheme`.

## Principales décisions et règles non-négociables

- `catalog/theme` : lecture seule, presets globaux, aucun event, aucun lifecycle.
- `core/tenanttheme` : logique tenant-scoped, apply/activation, events, versioning.
- L'API publique sous `catalog/theme/api` ne doit pas dépendre de `internal` — mapping via `internal/mapper`.
- Unicité global sur `code` des presets (contraintes DB) ; soft-delete via `deleted_at`.
- Caches longue durée pour lectures ; invalidation explicite sur writes administratifs.

## Plan de migration recommandé (ordre sécurisé)

1. Créer `catalog/theme` (API + internal) et migration SQL (table `theme_preset`).
2. Déployer `catalog/theme` (read endpoints) en parallèle sans retirer l'ancien module.
3. Créer `core/tenanttheme` (domain, commands, handlers, persistence `tenant_theme`).
4. Augmenter les handlers existants pour consommer `ThemePresetCatalog` et écrire tenant_theme via `core/tenanttheme`.
5. Basculer progressivement le trafic et les usages ; vérifier tests et intégrations.
6. Supprimer l'ancien module `theme` legacy une fois tout validé.

## Livrables (référence)

- `openspec/changes/catalog-theme-presets/tasks.md` — tâches d'implémentation détaillées
- `openspec/changes/catalog-theme-presets/design.md` — design technique détaillé
- `openspec/changes/catalog-theme-presets/specs/theme-preset/spec.md` — spécification formelle
- `openspec/changes/catalog-theme-presets/note.md` — plan de migration et décision d'architecture

## Validation initiale

Après approbation, lancer :

```bash
openspec validate catalog-theme-presets --strict --no-interactive
```

## Critères d'acceptation synthétiques

- `catalog/theme` expose `listActive()`, `findById`, `findByCode` en renvoyant uniquement les presets actifs et non supprimés.
- Contrôle d'unicité `code` en base + gestion d'erreurs lisibles au niveau du service admin.
- Tests unitaires et d'intégration (H2) verts ; ArchUnit empêchant fuites `api` → `internal`.
- Migration SQL testée et committée.

## Références

- `tchalanet-server/docs/ARCHITECTURE.md`
- `tchalanet-server/docs/NAMING.md`
- `tchalanet-server/docs/PLAYBOOK.md`
- `openspec/context/75-catalog-rules.md`
- `openspec/changes/catalog-theme-presets/note.md`
- `openspec/context/75-catalog-rules.md` (règles d'architecture des catalogs)
- unit tests dans `tchalanet-server/docs/conventions/testing.md`
- cache dans `tchalanet-server/docs/conventions/cache.md`
- timezon dans `tchalanet-server/docs/conventions/timezone.md`
- wrapper id dans `tchalanet-server/docs/conventions/typed_ids.md`
- command et query handlers dans `tchalanet-server/docs/conventions/command_query_handlers.md`
- itempootency dans `tchalanet-server/docs/conventions/itempotency.md`
- interdomain calls dans `tchalanet-server/docs/conventions/inter_domain_calls.md`
- event_model dans `tchalanet-server/docs/conventions/event_model.md`
- securoty et permissions dans `tchalanet-server/docs/conventions/security_permission.md`
- convention pour l'api dans `tchalanet-server/docs/conventions/api/*`
- Conventions de persistance/JPA présentes dans `tchalanet-server/docs/conventions/persistence/*`

---

Fin de la proposition
