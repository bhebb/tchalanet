# Tchalanet – Architecture Applicative (Server)

Ce document décrit l’architecture du backend **tchalanet-server**.

Objectifs :

- garder une architecture stable (hexagonal + CQRS) dans les domaines critiques
- permettre de livrer vite via des **vertical slices** (features)
- garder `common/` strictement technique
- clarifier le rôle de `catalog/` (référentiels)

> Règle d’or (pragmatique) :
>
> - Si ça touche **argent**, **tirages/résultats**, **tickets**, **fraude**, **limites**, **audit**, **sécurité**, **RLS** → `core/`
> - Si c’est **orchestration / page / BFF / agrégation multi-domaines** → `features/`
> - Si c’est **référentiel / lookup / tables partagées** (souvent “read-mostly”) → `catalog/`
> - Si c’est **technique transversal** → `common/`

---

## Table des matières

- [1. Structure générale](#1-structure-générale)
- [2. Module `common/` (transversal)](#2-module-common-transversal)
- [3. Module `catalog/` (référentiels)](#3-module-catalog-référentiels)
- [4. Module `core/` (hexagonal)](#4-module-core-hexagonal)
- [5. Module `features/` (vertical slices)](#5-module-features-vertical-slices)
- [6. API & scopes (routing)](#6-api--scopes-routing)
- [7. Sécurité, permissions et contexte (RequestContext)](#7-sécurité-permissions-et-contexte-requestcontext)
- [8. Persistence & RLS](#8-persistence--rls)
- [9. Batch & After-Commit](#9-batch--after-commit)
- [10. Cache (Caffeine + Redis)](#10-cache-caffeine--redis)
- [11. Erreurs & API response](#11-erreurs--api-response)
- [12. Conventions de placement (cheat sheet)](#12-conventions-de-placement-cheat-sheet)

---

## 1. Structure générale

Organisation logique (packages Java) :

```text
com.tchalanet.server/
├─ common/        # technique transversal (pas de métier)
├─ catalog/       # référentiels / lookup / SDR optionnel
├─ core/          # domaines critiques (hexagonal / CQRS)
└─ features/      # vertical slices / orchestration (BFF)
```

Important : features/ peut appeler plusieurs domaines (core/_, catalog/_) mais ne doit pas re-dupliquer de règles métier critiques.

---

## 2. Module `common/` (transversal)

`common/` = boîte à outils technique. Aucune logique métier.

Contient typiquement :

- Contexte : `TchRequestContext`, `TchContext`, `@CurrentContext`
- Sécurité technique : filtres, converters, headers constants (ex: `TchContextFilter`)
- Erreur : `ProblemRest`, `ProblemRestException`, handlers globaux
- Bus : `CommandBus`, `QueryBus`, interfaces handlers (`CommandHandler`, `QueryHandler`, etc.)
- Tx : `@TchTx`, `AfterCommit` (publication after-commit), utilitaires
- Cache technique : specs, key builder, wrappers
- Types : `...types.id.*` (wrappers d’ID), enums transversaux
- Web : pagination (`TchPage`), `ApiResponse` (si utilisé), converters Spring, etc.
- Persistence tech : utilitaires RLS, data sources bootstrap/bypass RLS

Règle : si un code contient des règles de validation métier, transitions de status, calculs de gains → ce n’est pas `common`.

---

## 3. Module `catalog/` (référentiels)

`catalog/` = référentiels “read-mostly” utilisés par plusieurs domaines.

Caractéristiques :

- peu/pas de logique métier
- mapping simple, souvent orienté lookup
- peut exposer des endpoints simples (admin CRUD)
- souvent global / multi-tenant “soft” (à cadrer selon table)

Exemples typiques :

- `catalog/resultslot` (slots globaux, seed, CRUD ops admin)
- `catalog/game` (registry/mapping de jeux et options, tenant-game catalog)
- `catalog/pricing` (si c’est un référentiel et non un calcul métier)

À éviter : placer ici des règles d’argent ou de settlement → ça reste `core/`.

Structure recommandée (souple) :

```text
catalog/<ref>/
├─ api/ or web/         # endpoints simples
├─ domain/              # modèle simple (ou records)
├─ application/         # queries/commands légers si besoin
├─ infra/persistence/   # JPA/JDBC
└─ internal/            # seed/bootstrap/maintenance (optionnel)
```

---

## 4. Module `core/` (hexagonal)

`core/` = domaines critiques. Architecture hexagonale + CQRS.

Structure standard :

```text
core/<bc>/
├─ domain/
│  ├─ model/
│  ├─ exception/
│  └─ service/ (optionnel)
├─ application/
│  ├─ command/
│  │    ├─ model/
│  │    └─ handler/
│  ├─ query/
│  │    ├─ model/
│  │    └─ handler/
│  └─ event/ (listeners applicatifs si besoin)
├─ port/
│  ├─ in/   (rare)
│  └─ out/  (persistence, cache, external, etc.)
└─ infra/
     ├─ persistence/
     ├─ web/          # controllers “core only” (si endpoints métiers directs)
     ├─ event/
     ├─ batch/
     ├─ cache/
     └─ security/     # helpers spécifiques domaine
```

Domaines typiques :

- `core.sales` : tickets, ventes, cancel/void, statuses, public verify ticket
- `core.payout` : demandes, approbation, paiement, split payment (si supporté)
- `core.ledger` : écritures, balances, reconciliation, reversals
- `core.draw` : draws tenant-scoped, attachement resultats global, lifecycle
- `core.limitpolicy` : limites + validations
- `core.accesscontrol` : permissions, roles, requirePermission / Authorized
- `core.audit` : audit events, Envers, journalisation

---

## 5. Module `features/` (vertical slices)

`features/` = orchestration/BFF/pages. Peut agréger plusieurs domaines.

Structure typique :

```text
features/<slice>/
├─ <Slice>Controller.java
├─ <Slice>Service.java   # orchestration
├─ dto/
├─ mapper/
└─ shared/ (optionnel)
```

Règle : pas de logique métier critique dupliquée. `features` orchestre, `core` décide.

---

## 6. API & scopes (routing)

Les endpoints appartiennent à un scope clair :

| Scope    | Prefix                    |
| -------- | ------------------------- |
| PUBLIC   | `/api/v1/public/**`       |
| TENANT   | `/api/v1/tenant/**`       |
| ADMIN    | `/api/v1/admin/**`        |
| PLATFORM | `/api/v1/platform/**`     |
| SDR      | `/_sdr/**` (servlet path) |

Notes :

- La politique d’accès est centralisée dans `SecurityConfig`.
- Les controllers restent "thin" (validation + mapping + appel handlers/services).
- OpenAPI : `Springdoc` configuré via `OpenApiConfig`.
- Sécurité globale : bearer JWT + OAuth2 (Keycloak) ; Swagger try-it-out possible.

---

## 7. Sécurité, permissions et contexte (RequestContext)

Contexte : publié par `TchContextFilter`, accessible via `@CurrentContext`, `TchRequestContext` ou `TchContext.get()`.

Ce contexte contient (au minimum) :

- tenant original/effective (code + uuid)
- user Keycloak sub + appUserId (bootstrap)
- roles systèmes + roles custom
- requestId, ip, user-agent, locale
- flags (tenant overridden), `deleted_visibility`

Permissions :

- `core.accesscontrol` fournit utilitaires/handlers (ex: check perms)
- annotations type `@Authorized` / `requirePermission` doivent être uniformes

RLS :

- RLS = dernière ligne de défense ; on ne “compte pas” dessus pour faire du routing logique
- S’assurer que le contexte est bien posé pour que RLS fonctionne

---

## 8. Persistence & RLS

Règles :

- Flyway obligatoire pour toute évolution de schéma
- `ddl-auto=validate`
- Tables tenant-scoped : RLS + index `tenant_id` + politiques cohérentes
- Envers si audit requis (config globale)

Wrappers d’ID (règle structurante) :

- Dans `domain/` et `application/` : utiliser des wrappers (ex `TenantId`, `TicketId`, etc.)
- UUID “brut” : accepté dans `JpaEntity`, `JpaRepository`, JDBC row mapping
- Conversions via MapStruct / mappers

---

## 9. Batch & After-Commit

- Batch : jobs critiques dans `core.<bc>.infra.batch`
- Pas d’auto-run au démarrage (`spring.batch.job.enabled=false`)
- Orchestration via scheduler ou endpoints d’opération
- After-commit : publication d’événements via `DomainEventPublisher`
- Listeners/side effects (ledger write, notifications, stats) déclenchés après commit
- Éviter les écritures cross-domain dans la même transaction si la fiabilité est requise (préférer events & replay)

---

## 10. Cache (Caffeine + Redis)

Cache 2 niveaux :

- L1 : Caffeine (process local)
- L2 : Redis (partagé)

Règles :

- TTL et specs fournis par un provider central (pas hard-codés dans handlers)
- Clés via `CacheKeyBuilder`
- Feature-flag possible pour activer Redis sans impacter le layout/contrats

---

## 11. Erreurs & API response

Erreurs :

- `ProblemRest` / `ProblemRestException` comme standard d’erreur
- Mapping centralisé (`@ControllerAdvice` / `ErrorHandler`)
- Pas de stacktrace exposée au client

Réponses :

- Si `ApiResponse` est utilisé : documenter le format et l’appliquer de façon cohérente
- Sinon garder DTO explicites — ne pas mixer plusieurs patterns sans décision

---

## 12. Conventions de placement (cheat sheet)

- `TchRequestContext`, converters, headers, `ProblemRest`, bus, tx, cache infra → `common/`
- Ticket, Payout, LedgerEntry, state machine, validations métier → `core/<bc>/domain`
- `SellTicketCommandHandler`, `ApprovePayoutCommandHandler` → `core/<bc>/application/command/handler`
- `TicketReaderPort`, `LedgerWriterPort` → `core/<bc>/port/out`
- `JpaTicketRepositoryAdapter`, `LedgerEntryJpaEntity` → `core/<bc>/infra/persistence`
- `PublicHomeController` qui agrège news + draws + plans → `features/publichome`
- Référentiel “slot”, “game registry”, “pricing table” → `catalog/<ref>`

---

_Document maintenu par l’équipe backend._
