# Tchalanet – Architecture Applicative (2025)

Ce document décrit l’architecture logicielle du backend Tchalanet telle qu’elle est appliquée en 2025.  
Elle repose sur deux piliers structurants :

1. **core/** — Domaines critiques (hexagonal / Clean Architecture)
2. **features/** — Vertical slices orientés écrans / use-cases UI

> Règle d’or :
>
> Si la fonctionnalité touche l’argent, la légalité, les tirages ou la fraude → placer dans `core/`.
> Sinon → mettre dans `features/`.

---

## Table des matières

- [Structure générale](#structure-g%C3%A9n%C3%A9rale)
- [Module `common/` (transversal)](#module-common-transversal)
  - [Handlers génériques](#handlers-g%C3%A9n%C3%A9riques)
  - [Stereotypes](#stereotypes)
  - [Cross-cutting](#cross-cutting)
- [Module `core/` (hexagonal)](#module-core-hexagonal)
  - [Structure d’un bounded context](#structure-dun-bounded-context)
  - [Domaines core actuels](#domaines-core-actuels)
- [Module `features/` (vertical slices)](#module-features-vertical-slices)
- [Contrôleurs](#contr%C3%B4leurs)
- [Batch](#batch)
- [Persistence](#persistence)
- [Avantages](#avantages)

---

## Structure générale

Le dépôt est organisé ainsi (exemple) :

```text
server/
├─ common/        # outils transversaux
├─ core/          # domaines cœur (hexagonal)
│  ├─ draw/
│  ├─ sales/
│  ├─ payout/
│  ├─ limitpolicy/
│  ├─ session/
│  ├─ tenant/
│  ├─ pos/
│  ├─ game/
│  ├─ ledger/
│  ├─ accesscontrol/
│  ├─ audit/
│  └─ external/
└─ features/      # vertical slices (UI, pages, dashboards, content)
   ├─ publichome/
   ├─ verification/
   ├─ news/
   ├─ stats/
   ├─ reporting/
   ├─ notifications/
   ├─ pagemodel/
   └─ vendordashboard/
```

Chaque partie a un rôle clair : `core/` contient tout ce qui est critique, `features/` compose des UI / pages / services non critiques et `common/` fournit des utilitaires techniques partagés.

---

## Module `common/` (transversal)

`common` contient les outils techniques réutilisables et les abstractions transversales. On n’y place pas de logique métier.

### Handlers génériques

On uniformise les handler interfaces dans `common/app/` :

- `CommandHandler<C, R>`
- `VoidCommandHandler<C>`
- `QueryHandler<Q, R>`

Exemples d'usage :

```java
public class CloseDueDrawsHandler
        implements VoidCommandHandler<CloseDueDrawsCommand> {}

public class GetDrawResultHandler
        implements QueryHandler<GetDrawResultQuery, DrawResult> {}
```

Ces interfaces remplacent les anciennes interfaces par use case (ex. `CloseDrawCommandHandler`, `ListDrawsQueryHandler`, ...).

### Stereotypes

Des annotations réutilisables pour clarifier les responsabilités :

- `@UseCase` → Application service (handlers)
- `@TchTx` → Démarque une méthode/bean transactionnel
- `@DomainService` → Service métier pur
- `@Mapper` → Mapping stable (ex. MapStruct)

### Cross-cutting

Utilitaires transverses présents dans `common/` :

- `common.time.TchClock` (source de vérité temporelle)
- `common.audit.*`
- `common.web.*`
- `common.persistence.*`
- `common.error.*`
- `common.context.*`

---

## Module `core/` (hexagonal)

Chaque sous-module de `core` est un bounded context critique. Tous suivent l’architecture hexagonale :

```
core/<bc>/
  ├─ domain/
  │   ├─ model/
  ├─ application/
  │   ├─ command/
  │   │    ├─ model/   # Command records
  │   │    └─ handler/ # CommandHandler / VoidCommandHandler
  │   └─ query/
  │        ├─ model/   # Query records
  │        └─ handler/ # QueryHandler
  ├─ port/
  │   └─ out/          # Interfaces pour la persistence, cache, APIs externes
  └─ infra/
       ├─ persistence/ # JPA, Redis, Meili, JDBC
       ├─ batch/       # Spring Batch tools
       └─ web/         # controllers “core only”
```

### Domaines core actuels

Quelques domaines majeurs placés dans `core/` :

- `core.draw`

  - génération des tirages
  - statut & state machine (PLANNED → OPEN → CLOSED → RESULTED → SETTLED)
  - résultats manuels / externes
  - batchs : fetch, settle, close

- `core.sales`

  - tickets / bets
  - sessions de caisse
  - offline sync
  - endpoint public signé pour `/ticket/:code`

- `core.payout`

  - calcul des gains
  - paiement des gagnants

- `core.limitpolicy`

  - plafonds
  - workflows de validation

- `core.ledger`

  - journalisation interne
  - mouvements comptables
  - soldes PDV / tenant

- `core.accesscontrol`, `core.session`, `core.tenant`, `core.theme`, etc.

---

## Module `features/` (vertical slices)

Les modules `features` représentent des vues / écrans ou fonctionnalités UI. Ils appellent les handlers du `core` et ne contiennent pas de règles métier critiques.

Exemple de structure d’un slice :

```
features/publichome/
   PublicHomeController.java
   PublicHomeService.java
   PublicHomePageModel.java
   mapper/
   dto/
```

Exemples de features :

- `features.publichome` — page publique, tirages du jour, news RSS
- `features.verification` — `/verifier`, `/ticket/:code` (compose `core.sales` + `core.draw`)
- `features.vendordashboard` — dashboard caissier, stats journalières
- `features.reporting` — rapports PDF/CSV

---

## Contrôleurs

Règles pour les controllers :

- Les controllers `features` injectent les handlers concrets (pas les interfaces génériques), pour garder la simplicité.
- Maximum recommandé : 2 `CommandHandler`, 1 `QueryHandler` par controller.
- Ne pas mettre de logique métier dans les controllers : validation / mapping + appel des handlers uniquement.

---

## Batch

Les batchs critiques sont placés dans `core.<bc>.infra.batch`. Exemples :

- batch fetch external results → `core.draw.infra.batch`
- batch settle draw
- batch close due draws

Avec Spring Batch 6 : configuration en Java (pas d’XML) — `@Bean Job`, `@Bean Step`, `ItemReader/ItemProcessor/ItemWriter` ou `Tasklet` pour actions simples.

---

## Persistence

- Dans `core/` on définit des `port.out` (interfaces) pour la persistence et les adaptateurs infra implémentent ces ports.
  - Exemple : `core.draw.port.out.DrawResultReaderPort` et `core.draw.infra.persistence.JpaDrawResultRepository`
- Mapping : Domain ↔ Entity dans `infra/` (ne pas importer entités dans le domaine).
- Request/Response ↔ DTO dans `features/web`.

---

## Bonnes pratiques et conventions

- Garder les règles métier dans `core/` et l’ordonnancement / UI dans `features/`.
- Limiter les dépendances entre bounded contexts. Préférer des ports/ports adapters.
- Favoriser les petites interfaces (Command/Query records + handlers) pour faciliter le test et la lecture.

---

## Avantages

- Domaines critiques isolés → facilité de tests et conformité.
- Features rapides à développer → vélocité pour les écrans et BFF.
- Architecture stable pour 300+ handlers et prête pour une future migration en microservices si nécessaire.

---

_Document maintenu en 2025 par l’équipe backend._
