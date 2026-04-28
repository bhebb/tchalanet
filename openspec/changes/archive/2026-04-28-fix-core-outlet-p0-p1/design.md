## Context

`core.outlet` est le bounded context qui gère les points de vente, leur cycle journalier (open/close)
et les configuration POS. L'audit du 28/04/2026 relève :

1. **`OutletReportController` dans le core** : le controller est dans `core/outlet/infra/web/admin/`
   avec un TODO explicite `"This reporting endpoint is TEMPORARY"`. Il expose des stats et des exports
   CSV — deux responsabilités qui appartiennent à des features différentes selon la règle architecturale
   (feature = BFF multi-domaines ; core = CRUD mono-domaine).

2. **`Outlet.addressId` en `UUID` brut** : le domaine manipule un UUID là où il devrait manipuler
   un `AddressId` typé — incohérence avec la convention typed IDs du projet.

3. **`CreateOutletCommandHandler` retourne `UUID`** : le handler typé devrait retourner `OutletId`.

4. **Handlers sans domain events** : `Close`, `Reopen`, `UpdateConfig` ne publient rien après commit —
   les features stats/pos ne peuvent pas réagir.

5. **`OutletView` avec UUIDs bruts** : `id` et `tenantId` ne sont pas typés.

6. **`GenerateOutletReportQueryHandler` ignore `query.to()`** : toujours rapport mono-jour.

## Goals / Non-Goals

**Goals:**

- `OutletReportController` supprimé du core et les fonctions migrées dans les bonnes features
- `Outlet.addressId` typé `AddressId` partout (domaine + infra + handlers)
- `CreateOutletCommandHandler` retourne `OutletId`
- 3 domain events publiés after-commit
- `OutletView` avec typed IDs
- `GenerateOutletReportQueryHandler` utilise from + to
- Fuite fichier `/tmp` corrigée

**Non-Goals:**

- Renommage `core.pos` → `core.terminal` (OpenSpec séparé non implémenté)
- Implémentation des listeners domain events (placeholder seulement)
- Modification du schéma DB outlet

## Decisions

### D1 — Split du controller selon la nature des endpoints

**Décision** :

- `OutletDailySummary` = stat opérationnelle live → **`features/stats/outlet_daily`**
  - Raison : c'est une métrique d'une journée pour un outlet spécifique, cohérente avec
    `cashier_dashboard` et `tenant_dashboard` dans `features/stats`
  - Path : `GET /tenant/stats/outlet/{id}/daily?date=YYYY-MM-DD`
- Génération/téléchargement CSV = rapport d'export → **`features/reporting/outletreport`**
  - Raison : `features/reporting` contient déjà `outletperformance`, `salesreport`, `tenantkpis`
  - Path : `GET /tenant/reports/outlet/{id}/export?from=X&to=Y`
  - Path : `GET /tenant/reports/outlet/{id}/download?date=X`

**Alternative rejetée** : `features/platformadmin/outlets/` — rejeté car la règle architecturale
interdit les features mono-domaine dans platformadmin ; les features tenantadmin/platformadmin
doivent être multi-domaines (BFF).

### D2 — `salesBlocked` non levé automatiquement à la réouverture

**Décision** : `ReopenOutletDayCommandHandler` ne lève PAS `salesBlocked` automatiquement.
Le blocage des ventes est une décision opérationnelle indépendante (ex. fraude, maintenance).
La réouverture ne signifie pas que les ventes peuvent reprendre.

Retour enrichi `ReopenOutletDayResult` avec `salesStillBlocked: boolean` pour que le frontend
puisse afficher un warning.

### D3 — `Outlet` refactorisé : `addressId` typed

**Décision** : changer le type de `UUID` à `AddressId` dans toutes les signatures du domaine.
La conversion `AddressId ↔ UUID` se fait dans `OutletPersistenceAdapter` (couche infra) — le
domaine ne connaît jamais `UUID` brut.

### D4 — `GenerateOutletReportQueryHandler` : multi-jours via CSV consolidé

**Décision** : si `from != to`, `FilesystemOutletReportAdapter` itère jour par jour et crée un
fichier CSV consolidé avec une colonne `date`. Pas de migration SQL — SQL adapté dans l'adapter.

### D5 — Fuite fichier `/tmp` : `Files.deleteIfExists` dans `finally`

**Décision** : après streaming du fichier au client, le supprimer dans un bloc `finally` en
wrappant `InputStreamResource` avec un `DeletingFileResource` custom (ou équivalent `ResponseEntity`
avec `InputStream` + `Files.delete` post-close).

## Risks / Trade-offs

- **[Risk] Changement de type `addressId` dans `Outlet`** : les tests existants qui instancient
  `Outlet` avec un `UUID` vont casser. → Mitigation : grep sur tous les usages et corriger en batch.

- **[Risk] `ReopenOutletDayCommandHandler` change de signature** (VoidCommandHandler → CommandHandler)
  : le controller appelant doit être adapté. → Mitigation : identifier le controller avant de changer.

- **[Risk] Changement de path HTTP** : `/platform/outlets/{id}/daily-summary` → `/tenant/stats/outlet/{id}/daily`
  — les clients qui utilisent les anciens paths devront s'adapter. → Acceptable (pas en production).

## Migration Plan

1. Créer les 3 domain events (D3)
2. Migrer `Outlet.addressId` → `AddressId` + corriger tous les usages
3. Corriger `OutletView` typed IDs
4. Corriger `CreateOutletCommandHandler` → retour `OutletId`
5. Ajouter domain events dans les 3 handlers
6. Corriger `ReopenOutletDayCommandHandler` → `ReopenOutletDayResult`
7. Corriger `GenerateOutletReportQueryHandler` + `OutletReportPort` + adapter
8. Créer `features/stats/outlet_daily/OutletDailyStatsController`
9. Créer `features/reporting/outletreport/OutletReportExportController`
10. Supprimer `core/outlet/infra/web/admin/OutletReportController.java`
11. Build + tests

## Open Questions

- _(aucune — toutes les décisions ont été tranchées)_
