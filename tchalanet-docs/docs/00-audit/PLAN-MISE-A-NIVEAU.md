# Plan de mise à niveau — Tchalanet

> Produit le 2026-04-24, basé sur AUDIT-EXISTANT.md et GAPS.md.
> À réviser à chaque sprint.

---

## Étape 0 — Corrections avant tout nouveau code (ordre strict)

Ces corrections doivent être fusionnées **avant** toute nouvelle feature. Elles évitent des pannes de démarrage, des conflits de merge, et des violations bloquantes qui polluent le code à venir.

| #   | Correction                                                                                                                                                                                           | Fichier(s)                                                               | Effort | Priorité     |
| --- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------ | ------ | ------------ |
| 1   | **Résoudre le conflit Flyway V16** — renommer `V16__stats_draw.sql` → `V46__stats_draw.sql` (V45 libre)                                                                                              | `src/main/resources/db/migration/V16__stats_draw.sql`                    | S      | 🔴 CRITIQUE  |
| 2   | **Résoudre le conflit Flyway V22** — renommer `V22__tchala.sql` → `V47__tchala.sql`                                                                                                                  | `src/main/resources/db/migration/V22__tchala.sql`                        | S      | 🔴 CRITIQUE  |
| 3   | **Supprimer `@RepositoryRestResource`** sur `UserPreferenceRestRepository`, `PayoutRestRepository`, `features/pagemodel/PageModelRepository` — exposent des entités JPA sans contrôle                | 3 fichiers                                                               | S      | 🔴 CRITIQUE  |
| 4   | **Supprimer `features/pagemodel_backup/`** — dead code non utilisé                                                                                                                                   | `features/pagemodel_backup/` (répertoire entier)                         | S      | 🟠 IMPORTANT |
| 5   | **Unifier les doublons PageModelController** — garder `features/pagemodelruntime/`, supprimer `features/pagemodel/PageModelController.java` ; corriger la route hardcodée `/api/v1/public/pagemodel` | 2 fichiers                                                               | M      | 🟠 IMPORTANT |
| 6   | **Supprimer le doublon `TenantAdminPoliciesLimitsController`** — 2 classes dans des sous-packages différents                                                                                         | `features/tenantadmin/policies/TenantAdminPoliciesLimitsController.java` | S      | 🟠 IMPORTANT |
| 7   | **Migrer constructor injection** dans `LimitPolicyRuntimeService` (×2) et `DrawMapper`                                                                                                               | 2 fichiers                                                               | S      | 🟠 IMPORTANT |
| 8   | **Migrer vers `ApiResponse<T>`** dans `TicketController`, `PublicTicketController`, `TenantThemeController`                                                                                          | 3 fichiers                                                               | S      | 🟠 IMPORTANT |
| 9   | **Remplacer `@Data`** Lombok sur `TenantGameView`, `UpdatePolicyRequest`, `DrawChannelResponse`, `Stats*Entity` (×3)                                                                                 | 6 fichiers                                                               | S      | 🟡 MINEUR    |
| 10  | **Supprimer `InsecureJwtDecoderConfig.java`** si c'est du code de dev (javax.net.ssl) — ou déplacer sous profile `local` uniquement                                                                  | 1 fichier                                                                | S      | 🟡 MINEUR    |

**Effort total étape 0** : ~3 jours

---

## Étape 1 — Proposals OpenSpec à créer (ordre de dépendance)

> Chaque item ci-dessous nécessite une proposal approuvée dans `openspec/changes/` avant toute implémentation.

| change-id            | Périmètre                                                                                                      | Dépend de                              | Effort |
| -------------------- | -------------------------------------------------------------------------------------------------------------- | -------------------------------------- | ------ |
| **#82** _(en cours)_ | TenantAdmin — gestion des utilisateurs Keycloak (CRUD, invitations, rôles)                                     | `core/tenantuser` + Keycloak admin API | M      |
| **#83** _(en cours)_ | TenantAdmin — configuration tenant (identité, settings, i18n)                                                  | #82                                    | M      |
| **#84** _(en cours)_ | TenantAdmin — gestion des points de vente (outlets)                                                            | #82                                    | M      |
| **#85** _(en cours)_ | TenantAdmin — gestion des terminaux POS                                                                        | #84                                    | S      |
| **#86** _(en cours)_ | TenantAdmin — gestion des tirages (calendar, results)                                                          | —                                      | M      |
| **#87** _(à créer)_  | Ledger RLS — ajouter policy RLS sur `ledger_entry`, supprimer les `findByTenantId*` du JpaAdapter              | Étape 0 complète                       | M      |
| **#88** _(à créer)_  | Typed IDs pour `core/ledger` — remplacer UUID brut par `LedgerId`, `LedgerEntryId`                             | #87                                    | S      |
| **#89** _(à créer)_  | Typed IDs pour `core/haiti` — remplacer UUID brut dans domain/application                                      | —                                      | S      |
| **#90** _(à créer)_  | Page model runtime — unifier `features/pagemodel` + `features/pagemodelruntime` en une seule feature cohérente | Étape 0 item #5                        | L      |
| **#91** _(à créer)_  | Tests critiques core — ajouter couverture sur `sales`, `draw`, `payout`, `ledger` (Testcontainers)             | —                                      | L      |

---

## Ce qui est réutilisable tel quel

Le code suivant respecte déjà les conventions et peut être utilisé comme référence ou base pour les prochains développements.

### Backend — Patterns exemplaires

| Composant                                      | Localisation                                                | Pourquoi c'est une bonne référence                                           |
| ---------------------------------------------- | ----------------------------------------------------------- | ---------------------------------------------------------------------------- |
| RLS infrastructure                             | `common/config/DataSourceConfig`, `common/persistence/rls/` | TchContextFilter + RlsAwareDataSource = implémentation canonique             |
| `SellTicketCommandHandler`                     | `core/sales/application/command/handler/`                   | Handler CQRS avec `@TchTx`, typed IDs, AfterCommit events                    |
| Catalog `drawchannel`                          | `catalog/drawchannel/`                                      | Structure catalog complète avec api/internal, cache, mapper, web, read/write |
| `TicketEntity` + `TicketLineEntity`            | `core/sales/infra/persistence/`                             | Entités JPA avec `BaseTenantEntity`, UUID uniquement dans persistence        |
| `ResultSlotJpaAdapterTest`                     | `test/catalog/resultslot/`                                  | Exemple de test JPA adapter                                                  |
| `ArchitectureTest` + `FeatureArchitectureTest` | `test/`                                                     | Enforcement ArchUnit des règles de couche — à étendre                        |
| V40 + V41 RLS policies                         | `db/migration/`                                             | Pattern canonique pour politique RLS tenant                                  |

### Frontend — Patterns exemplaires

| Composant                              | Localisation                               | Pourquoi c'est une bonne référence                        |
| -------------------------------------- | ------------------------------------------ | --------------------------------------------------------- |
| `libs/shared/auth/`                    | AuthService, authGuard, roleGuard          | Intégration Keycloak OIDC complète, standalone            |
| `libs/shared/data-access/session/`     | sessionFeature NgRx                        | Store feature complet (actions/reducer/effects/selectors) |
| `libs/shared/data-access/page/`        | page + nav stores                          | Store feature avec navigation page model                  |
| App routes                             | `apps/tchalanet-web/src/app/app.routes.ts` | lazy loading, provideState par route, authGuard           |
| `PrivateShellComponent` + role routing | `libs/web/private-pages/`                  | Dashboard routing par rôle avec canMatch                  |
| `libs/ui/widget-renderer/`             | Widget renderer                            | Moteur de rendu page model — ne pas dupliquer             |

---

## Résumé exécutif

```
═══════════════════════════════════════════════════════════════
  ÉTAT GÉNÉRAL              ██ ORANGE
  ─────────────────────────────────────────────────────────────
  Modules backend (core)  : 26 domaines
  Modules backend (catalog): 10 modules de référence
  Controllers              : 65 (dont 4 doublons à supprimer)
  Entités JPA              : 46 entités

  Migrations Flyway        : 42 fichiers (V1–V51)
  ⚠  Conflits de version   : 2 bloquants (V16 ×2, V22 ×2)

  Tests                    : 14 fichiers (~13 tests réels)
  Couverture estimée       : < 10%
  Tests manquants critiques: sales, draw, payout, ledger

  Violations bloquantes    : 5 (Flyway ×2 + @RepositoryRestResource ×3)
  Violations importantes   : 15
  Violations mineures      : 5

  Couverture MVP Phase 0   : ~65%
  ─────────────────────────────────────────────────────────────
  PROCHAINES ACTIONS :
  1. Corriger les 2 conflits Flyway (V16, V22) — sans ces corrections
     l'application ne démarre pas
  2. Supprimer les 3 @RepositoryRestResource — exposition non contrôlée
  3. Lancer OpenSpec #87 (Ledger RLS) en parallèle des corrections
═══════════════════════════════════════════════════════════════
```
