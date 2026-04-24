---
name: backend-architecture
description: >
  Use when creating, moving, or reviewing Java classes in tchalanet-server — enforces the 4-layer architecture (common/catalog/core/features), package structures for core/catalog/feature modules, dependency graph rules, and the Rule of 3 for sub-packages.
---

# Backend Architecture — Couches et packages

> ⚠️ **Ce fichier est un résumé actionable pour l'IA.**
> Ne pas éditer ce fichier pour changer une règle — modifier la source canonique :
> 👉 `tchalanet-server/docs/ARCHITECTURE.md`

## Couches (STRICTES)

```
common/      → technique transversal uniquement (bus, context, errors, cache infra)
catalog/     → référentiels read-mostly (lookup, configuration, calendriers)
core/        → domaines métier critiques (ventes, tirages, argent, limites, audit)
features/    → orchestration / BFF / vertical slices / composition multi-domaines
```

## Graphe de dépendances (NON-NÉGOCIABLE)

```
common
  ↑
catalog     core
    ↑       ↑
     └── features
```

- `core/` ❌ ne dépend jamais de `features/` ni de `catalog/`
- `catalog/` ❌ jamais de side-effects, jamais de domain events
- `features/` ✔ orchestre `core/` et lit `catalog/`
- `common/` ❌ ne dépend jamais du métier

Toute violation → **ADR obligatoire** (`tchalanet-docs/docs/03-adr/`).

---

## Structure interne d'un module `core/`

```
core/<domain>/
├─ domain/
│  ├─ model/          ← aggregates, entities, value objects (PURE, framework-free)
│  ├─ exception/
│  └─ service/        ← domain services (logique pure, pas d'IO)
├─ application/
│  ├─ command/
│  │  ├─ model/       ← XxxCommand (record, typed IDs)
│  │  └─ handler/     ← XxxCommandHandler (un handler = une commande)
│  ├─ query/
│  │  ├─ model/       ← XxxQuery (record)
│  │  └─ handler/     ← XxxQueryHandler
│  └─ event/          ← application events
├─ port/
│  └─ out/            ← output ports uniquement (XxxReaderPort, XxxWriterPort)
└─ infra/
   ├─ persistence/    ← JPA entities, repositories (UUID autorisé ici uniquement)
   ├─ web/            ← controllers HTTP (thin, délèguent au bus)
   ├─ batch/          ← schedulers (orchestration uniquement, pas de logique)
   ├─ event/          ← listeners infra (@TransactionalEventListener AFTER_COMMIT)
   └─ cache/          ← adapters cache
```

**Règles domain layer** :

- Framework-free et déterministe
- Pas d'accès aux repositories ni aux ports
- Typed IDs exclusivement (pas de UUID brut)
- Pas de `@Component`, `@Service`, `@Transactional`

---

## Structure interne d'un module `catalog/`

```
catalog/<name>/
├─ api/
│  ├─ XCatalog.java           ← interface read-only (side-effect free)
│  └─ model/
│     ├─ XView.java           ← projection complète
│     ├─ XSummaryView.java    ← projection légère
│     ├─ XRow.java            ← projection use-case spécifique
│     └─ XSearchCriteria.java
└─ internal/
   ├─ read/       ← XCatalogImpl (implements XCatalog, @Cacheable)
   ├─ write/      ← XAdminService (CRUD admin + @CacheEvict)
   ├─ mapper/     ← Entity → View (MapStruct)
   ├─ persistence/← JPA entities + repositories
   ├─ cache/      ← noms de cache (catalog:<name>:active, by_id, by_key)
   └─ web/        ← controllers admin (SUPER_ADMIN / TENANT_ADMIN seulement)
```

**Règles catalog** :

- `api/` MUST NOT dépendre de `internal/`
- `core/` MAY lire via les interfaces `XCatalog`, jamais via `internal/`
- Pas de domain events métier depuis `catalog/`
- Pas de business invariants

---

## Structure interne d'une `feature/`

```
features/<feature_key>/
└─ <slice_key>/        ← un slice = une zone UI / entrée de navigation
   ├─ web/             ← XxxController (HTTP boundary, thin)
   ├─ app/             ← XxxService / XxxOrchestrator
   ├─ model/           ← XxxRequest, XxxResponse, XxxView, XxxItem
   ├─ mapper/          ← XxxMapper / XxxWebMapper
   ├─ dynamic/         ← providers/plug-ins (optionnel)
   └─ shared/          ← helpers internes (optionnel)
```

**Règle des 3** : un sous-package est créé **uniquement si ≥ 3 classes** l'occupent.

**Règles feature** :

- Orchestre — ne décide jamais (les invariants restent dans `core/`)
- Pas d'accès aux repositories ni aux JPA entities
- Pas de logique métier dans les controllers
- Umbrella feature → sous-slices obligatoires (pas de mega-service)

---

## Mental model (TL;DR)

| Couche      | Rôle                      |
| ----------- | ------------------------- |
| `domain/`   | Règles et invariants      |
| `command/`  | Intent de mutation        |
| `query/`    | Question (lecture)        |
| `event/`    | Fait passé                |
| `port/`     | Contrat de dépendance     |
| `infra/`    | Implémentation du contrat |
| `catalog/`  | Données de référence      |
| `features/` | Orchestration UI          |
| `common/`   | Glue technique            |
