# `tchalanet-common` — Shared Technical Kernel

Noyau technique transverse. Ne contient **pas** de logique métier.

> Règle d'or : si une classe n'est utilisée que dans un seul domaine, elle reste dans ce domaine.  
> `common` ne reçoit un type que quand au moins deux domaines indépendants en ont besoin.

---

## Packages

| Package | Rôle |
|---|---|
| `bus` | `CommandBus`, `CommandHandler`, `HandlerTypeResolver` — dispatch commands/queries |
| `cache` | `CacheSpec`, `CacheKeyBuilder`, `CacheSpecProvider` — convention cache Redis |
| `constant` | Constantes applicatives globales |
| `context` | `TchRequestContext`, `TchContextFilter`, `TchContextBinder`, resolvers, operational context, scope, tenant, auth, JWT |
| `crypto` | Utilitaires crypto transverses |
| `event` | `DomainEvent`, `DomainEventPublisher` — publication d'événements domaine |
| `exception` | Exceptions génériques (`DomainException`, validation, not-found, conflict) |
| `http` | Composants HTTP transverses |
| `job` | Support jobs/batch transverses |
| `json` | Configuration Jackson globale |
| `mapper` | Utilitaires de mapping transverses |
| `persistence` | `BaseEntity`, `BaseTenantEntity`, `AuditableEntity`, converters JPA |
| `security` | Configuration Spring Security, Keycloak, filtres JWT |
| `spring` | Configuration Spring Boot globale (CORS, OpenAPI, Actuator) |
| `stereotype` | Annotations transverses (`@DomainService`, etc.) |
| `time` | `ClockPort`, utilitaires timezone/date |
| `tx` | Support transactionnel transverse |
| `types/id` | Typed IDs transverses — voir `docs/conventions/typed_ids.md` |
| `types/money` | `Money`, `Currency` — types monétaires |
| `web` | `ApiResponse`, `GlobalExceptionHandler`, résolveurs MVC |

---

## Règles

**Ce qui appartient à `common` :**
- Infrastructure technique réutilisée par ≥ 2 domaines indépendants
- Contrats techniques (`CommandBus`, `DomainEventPublisher`, `ClockPort`)
- Types fondamentaux (`TchRequestContext`, Typed IDs, `Money`)
- Configuration Spring globale

**Ce qui n'appartient pas à `common` :**
- Entités et value objects métier spécifiques (`Ticket`, `Draw`, `Outlet`…)
- Repositories métier
- Use cases métier
- Contrôleurs REST liés à un domaine

---

## Context

Le sous-package `context` est le plus sensible — lire obligatoirement :

- [`docs/conventions/context/request-context.md`](../../../../../../docs/conventions/context/request-context.md) — contexte universel, pipeline HTTP, batch
- [`docs/conventions/context/operational-context.md`](../../../../../../docs/conventions/context/operational-context.md) — contexte POS/terrain
- [`docs/conventions/context/role-flows.md`](../../../../../../docs/conventions/context/role-flows.md) — flows par rôle

---

## Dépendances autorisées

`tchalanet-common` ne dépend d'aucun autre module Tchalanet.  
Tous les autres modules peuvent dépendre de `tchalanet-common`.

```
tchalanet-common ← tchalanet-catalog
tchalanet-common ← tchalanet-platform
tchalanet-common ← tchalanet-core
tchalanet-common ← tchalanet-features
```
