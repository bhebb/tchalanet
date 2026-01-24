# Tasks — game-tenantgame-split

## Objectif

Séparer complètement le référentiel **Game** (catalog) de la configuration et
du lifecycle **TenantGame** (core), sans régression fonctionnelle.

---

## 1 — Catalog cleanup (`catalog/game`)

- [x] 1.1 Supprimer toute logique tenant-scoped de `catalog/game`
  - `TenantGameCatalog`
  - `TenantGameEnsureService`
  - endpoints `/admin/tenant/games`
- [x] 1.2 Vérifier que `catalog/game` ne contient plus que :
  - `GameCatalog` (read-only)
  - `GameAdminService` (admin CRUD)
  - `GameJpaEntity`, `GameJpaRepository`
- [x] 1.3 Vérifier que :
  - aucun événement n’est émis
  - aucune table tenant n’est référencée
  - mapping reste interne

---

## 2 — Core tenantgame scaffolding

- [x] 2.1 Créer module `core/tenantgame`
  - `core/tenantgame/application/command`
  - `core/tenantgame/application/query`
  - `core/tenantgame/application/port`
  - `core/tenantgame/domain`
  - `core/tenantgame/infra/persistence`
  - `core/tenantgame/infra/web`
- [x] 2.2 Définir le modèle domain `TenantGame`
  - tenantId
  - gameCode
  - enabled
  - policies (jsonb ou fields)
  - version
  - audit

---

## Phase 3 — Persistence tenant-scoped

- [x] 3.1 Créer table `tenant_game` (RLS activée)
- [x] 3.2 Déplacer `TenantGameJpaEntity` et repository vers `core/tenantgame/infra/persistence`
- [x] 3.3 Créer `TenantGamePersistencePort`
- [x] 3.4 Implémenter `TenantGamePersistenceAdapter`

---

## Phase 4 — Commands (lifecycle)

- [x] 4.1 `EnsureTenantGamesCommand` → ajouter `tenantId`
- [x] 4.2 Implémenter `EnsureTenantGamesCommandHandler`
  - valider via `GameCatalog.findByCode`
  - persister tenant-scoped
  - idempotent
- [x] 4.3 Implémenter :
  - `EnableTenantGameCommand`
  - `DisableTenantGameCommand`
  - `UpdateTenantGamePolicyCommand`
- [ ] 4.4 Publier `TenantGameUpdatedEvent` after-commit (si requis)

---

## Phase 5 — Queries (read side)

- [x] 5.1 Créer `ResolveTenantGamesQuery`
- [x] 5.2 Implémenter `ResolveTenantGamesQueryHandler`
- [ ] 5.3 Retourner `TenantGameView` (DTO)
- [ ] 5.4 Enrichissement optionnel via `GameCatalog` (API only)

---

## Phase 6 — Web / Admin endpoints

- [x] 6.1 Déplacer `TenantGameAdminController` vers `core/tenantgame/infra/web`
- [x] 6.2 Modifier endpoints :
  - utiliser `gameCode` plutôt que `gameId`
  - ne jamais exposer domain entities
- [x] 6.3 Appliquer permissions tenant-admin / manager

---

## Phase 7 — Tests & Guards

- [x] 7.1 Unit tests command handlers (idempotence)
  - EnableTenantGameCommandHandlerTest: tests validation, idempotency
- [ ] 7.2 Integration tests (RLS + ensure/update) - FUTURE ENHANCEMENT
- [x] 7.3 ArchUnit:
  - [x] interdire `core → catalog.internal`
  - [x] interdire controllers retournant domain entities
  - [x] interdire events dans catalog
  - [x] catelog/game ne dépend pas de core/tenantgame

---

## Phase 8 — Documentation

- [ ] 8.1 Mettre à jour `DOMAIN_GAME.md`
- [ ] 8.2 Mettre à jour `PLAYBOOK.md`
- [ ] 8.3 Ajouter note de migration (si données existantes)

---

## Done when

- [ ] `catalog/game` est un catalog pur
- [ ] `core/tenantgame` gère tout le lifecycle tenant
- [ ] Aucun tenant state n’existe hors core
- [ ] Bootstrap peut consommer tenant games sans accès direct DB
