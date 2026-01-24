# Proposal: game-tenantgame-split

**Change-id**: `game-tenantgame-split`
**Status**: APPROVED
**Audience**: Backend developers, reviewers, Copilot
**Scope**: `catalog/game` + `core/tenantgame`

---

## Résumé

Ce changement introduit une séparation architecturale stricte entre :

- **Game** : référentiel global des jeux vendus (catalog)
- **TenantGame** : activation, configuration et règles effectives par tenant (core)

Objectif : éliminer toute logique tenant-scoped et tout lifecycle métier du catalog,
et centraliser les décisions opérationnelles dans le core.

---

## References

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

## Motivation

L’état actuel mélange :

- définitions globales de jeux
- activation/désactivation tenant
- paramètres métier (limites, flags, display)
- endpoints admin tenant-scoped

Ce mélange crée :

- un couplage fort entre catalog et core
- des violations RLS
- des catalogs non cacheables
- des frontières floues empêchant l’évolution indépendante

---

## Décisions (normatives)

### D1 — `catalog/game` est un référentiel pur

`catalog/game` MUST :

- stocker les définitions globales de jeux (code, nom, type, metadata)
- exposer une API read-only (`GameCatalog`)
- être read-mostly et cacheable
- filtrer les entrées soft-deleted
- exposer les jeux inactifs (avec `active=false`) via `findByCode`

`catalog/game` MUST NOT :

- gérer l’activation par tenant
- stocker des règles métier tenant-scoped
- émettre des événements
- orchestrer des workflows
- exposer des entités JPA

**Entité `GameJpaEntity` (référence) :**

- `code` (unique, length 32)
- `name` (length 128)
- `category` (length 32, ex: HAITI)
- `combination` (length 32)
- `minDigits`, `maxDigits`
- `description`
- `active` (boolean)
- `sortOrder` (int)

---

### D2 — `core/tenantgame` gère le lifecycle tenant

`core/tenantgame` MUST :

- gérer l’activation/désactivation de jeux par tenant
- stocker les politiques tenant-scoped (limites, flags, commissions, canaux)
- exposer des commands idempotentes
- exposer des queries de résolution effective
- publier des événements after-commit si nécessaire
- appliquer RLS strictement

**Entité `TenantGameJpaEntity` (tenant-scoped) :**

- `game_id` (FK vers `GameJpaEntity`, mais join interdit en domain logic)
- `enabled` (boolean)
- `displayName` (override optionnel)
- `minStake`, `maxStake` (BigDecimal)
- `flags` (JSONB pour configuration flexible)

---

### D3 — Frontières strictes catalog / core

- `core/tenantgame` MUST valider l’existence des jeux via `GameCatalog` (API publique)
- `core/tenantgame` MUST NOT dépendre de `catalog/game/internal/**`
- `catalog/game` MUST NOT dépendre de `core/tenantgame`
- Aucun join DB entre `game` et `tenant_game` (sauf contrainte FK technique si nécessaire, mais éviter les fetch eager cross-module)

---

## Portée

### Inclus

- Refactor des endpoints `/admin/tenant/games` vers `core/tenantgame`
- Déplacement de `tenant_game` persistence vers core
- Suppression de toute logique tenant dans `catalog/game`
- Ajout des guards ArchUnit

### Exclus

- Logique de vente (features/sell-ticket)
- Règles de validation de tickets
- Migration automatique de données historiques

---

## Critères d’acceptation

- `catalog/game` est cacheable et sans lifecycle
- `core/tenantgame` est tenant-scoped (RLS)
- Les commands sont idempotentes
- Les controllers n’exposent jamais d’entités domain
- Les guards ArchUnit passent

---

## Validation

```bash
mvn test -Dtest=ArchitectureTest
mvn test -Dtest=TenantGameCommandHandlerTest
```
