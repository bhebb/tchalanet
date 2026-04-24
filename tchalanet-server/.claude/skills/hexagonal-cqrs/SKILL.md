---
name: hexagonal-cqrs
description: >
  Déclencher pour toute création de module, feature, ou refactoring architectural.
  Indispensable si la tâche concerne : couches applicatives, graphe de dépendances,
  ports, adapters, commands, queries, handlers, domain services, ou positionnement
  d'une classe dans common / catalog / core / features.
---

# Architecture hexagonale + CQRS — Tchalanet

## Les 4 couches (STRICTES)

```
common/    → technique transversal uniquement (bus, context, errors, cache infra)
catalog/   → référentiels read-mostly (lookup, configuration, calendriers)
core/      → domaines métier critiques (ventes, tirages, argent, limites, audit)
features/  → orchestration / BFF / vertical slices / composition multi-domaines
```

## Graphe de dépendances (NON-NÉGOCIABLE)

```
common
  ↑
catalog     core
    ↑       ↑
     └── features
```

| Règle       | Détail                                             |
| ----------- | -------------------------------------------------- |
| `core/`     | ❌ ne dépend jamais de `features/` ou `catalog/`   |
| `catalog/`  | ❌ jamais de side-effects, jamais de domain events |
| `features/` | ✔ orchestre `core/` et lit `catalog/`              |
| `common/`   | ❌ ne contient jamais de logique métier            |

## Structure interne — module `core/<domain>/`

```
core/<domain>/
├─ domain/
│  ├─ model/       ← aggregates, entities, value objects (PURE, framework-free)
│  ├─ exception/
│  └─ service/     ← domain services (logique pure, pas d'IO)
├─ application/
│  ├─ command/
│  │  ├─ model/    ← XxxCommand (record + typed IDs)
│  │  └─ handler/  ← XxxCommandHandler (1 handler = 1 commande)
│  ├─ query/
│  │  ├─ model/    ← XxxQuery (record)
│  │  └─ handler/  ← XxxQueryHandler
│  └─ event/       ← application events
├─ port/
│  └─ out/         ← output ports uniquement (XxxReaderPort, XxxWriterPort)
└─ infra/
   ├─ persistence/ ← JPA entities, repositories (UUID autorisé ICI UNIQUEMENT)
   ├─ web/         ← controllers HTTP (thin, délèguent au bus)
   ├─ batch/       ← schedulers (orchestration uniquement, pas de logique)
   ├─ event/       ← listeners (@TransactionalEventListener AFTER_COMMIT)
   └─ cache/       ← adapters cache
```

## Structure interne — module `features/<key>/<slice>/`

```
features/<feature_key>/
└─ <slice_key>/        ← un slice = une zone UI/navigation
   ├─ web/             ← XxxController (HTTP boundary, thin)
   ├─ app/             ← XxxService / XxxOrchestrator
   ├─ model/           ← XxxRequest, XxxResponse, XxxView, XxxItem
   ├─ mapper/          ← XxxMapper / XxxWebMapper
   ├─ dynamic/         ← providers/plug-ins (optionnel)
   └─ shared/          ← helpers internes (optionnel)
```

## Règles CQRS

```java
// Command = record + typed IDs + intention claire
public record SellTicketCommand(TicketId ticketId, TenantId tenantId, ...) {}

// Handler = 1 commande = 1 handler, annoté @TchTx
@TchTx
public Result handle(SellTicketCommand cmd) { ... }

// Query = record léger
public record GetTicketQuery(TicketId ticketId) {}

// QueryHandler = lecture seule, pas de side-effects
public TicketDetails handle(GetTicketQuery query) { ... }
```

## Règle des 3

Un sous-package n'est créé que si **≥ 3 classes** l'occupent.

## Nommage des classes

| Rôle            | Pattern                                               |
| --------------- | ----------------------------------------------------- |
| Command         | `XxxCommand` (record)                                 |
| Command handler | `XxxCommandHandler`                                   |
| Query           | `XxxQuery` (record)                                   |
| Query handler   | `XxxQueryHandler`                                     |
| Output port     | `XxxReaderPort`, `XxxWriterPort`, `XxxRepositoryPort` |
| JPA adapter     | `XxxJpaAdapter`                                       |
| JPA entity      | `XxxJpaEntity`                                        |
| JPA repository  | `XxxJpaRepository`                                    |
| Domain event    | `XxxCreatedEvent`, `XxxCancelledEvent` (passé)        |
| Feature service | `XxxService` / `XxxOrchestrator`                      |
| Scheduler       | `XxxScheduler` — méthodes `tick()` / `runOnce()`      |

## Checklist avant de créer une classe

- [ ] Dans quelle couche va-t-elle ? (common / catalog / core / features)
- [ ] Respecte-t-elle le graphe de dépendances ?
- [ ] Le nommage suit-il le pattern ci-dessus ?
- [ ] Si core : la couche domain est-elle framework-free ?
- [ ] Si handler : est-il annoté `@TchTx` ?
- [ ] Si sous-package : y a-t-il ≥ 3 classes ?
