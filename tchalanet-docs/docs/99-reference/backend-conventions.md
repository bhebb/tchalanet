# Guide développeur backend

## Ce que cette page répond

Je travaille sur `tchalanet-server`. Quelles sont les règles à suivre et où les trouver ?

---

## Par où commencer

| Besoin | Document |
|---|---|
| Comprendre l'architecture en couches | [Architecture backend](../server-docs/ARCHITECTURE.md) |
| Guide pratique du quotidien | [Backend Playbook](../server-docs/PLAYBOOK.md) |
| Comprendre les domaines métier | [Index fonctionnel](../02-functional/index.md) |

---

## Conventions les plus utilisées

| Convention | Fichier |
|---|---|
| Command / Query handlers | [command_query_handlers.md](../server-docs/conventions/command_query_handlers.md) |
| Typed IDs | [typed_ids.md](../server-docs/conventions/typed_ids.md) |
| Web API (REST) | [web_api.md](../server-docs/conventions/api/web_api.md) |
| Request context (opérationnel) | [request-context.md](../server-docs/conventions/context/request-context.md) |
| Bus & Handlers | [bus.md](../server-docs/conventions/bus.md) |
| Clean Architecture (couches) | [clean_architecture.md](../server-docs/conventions/clean_architecture.md) |

---

## Persistence et données

| Convention | Fichier |
|---|---|
| Persistence (JPA, repositories) | [persistence.md](../server-docs/conventions/persistence/persistence.md) |
| Row-Level Security (RLS) | [rls.md](../server-docs/conventions/persistence/rls.md) |
| JPA entities | [jpa_entities.md](../server-docs/conventions/persistence/jpa_entities.md) |
| Audit persistence | [audit.md](../server-docs/conventions/persistence/audit.md) |

---

## Conventions spécialisées

| Convention | Fichier |
|---|---|
| Inter-domain calls | [inter_domain_calls.md](../server-docs/conventions/inter_domain_calls.md) |
| Event model | [event_model.md](../server-docs/conventions/event_model.md) |
| Idempotency | [idempotency.md](../server-docs/conventions/idempotency.md) |
| Security & permissions | [security_permissions.md](../server-docs/conventions/security_permissions.md) |
| Testing | [testing.md](../server-docs/conventions/testing.md) |
| Cache | [cache.md](../server-docs/conventions/cache.md) |
| Timezone | [timezone.md](../server-docs/conventions/timezone.md) |
| API response format | [api_response.md](../server-docs/conventions/api/api_response.md) |
| Routing & paths | [routing_and_path.md](../server-docs/conventions/api/routing_and_path.md) |
| Pagination | [pagination.md](../server-docs/conventions/api/pagination.md) |

---

## Règles non négociables

Voir `tchalanet-server/openspec/context/10-non-negotiables.md` pour les contraintes d'architecture dures (couches, dépendances, isolation tenant, etc.).

---

## Où vit la vérité

```
Code + tests verts  >  ADR  >  ARCHITECTURE.md  >  docs/conventions/  >  DOMAIN_*.md near-code
```

Voir [Où vit la vérité](../00-overview/where-truth-lives.md).
