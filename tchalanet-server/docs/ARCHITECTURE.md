# Tchalanet – Architecture Applicative (Server)

Ce document décrit l'architecture du backend **tchalanet-server**.
Il est **structurel** : frontières, responsabilités, règles non négociables.
Les détails d'implémentation et patterns concrets vivent dans `docs/conventions/*`.

## Objectifs

- Stabiliser l'architecture (hexagonal + CQRS) dans les domaines critiques (`core/`)
- Livrer vite via des **vertical slices** (`features/`)
- Garder `common/` strictement technique
- Clarifier le rôle de `catalog/` (référentiels read-mostly)
- Uniformiser le **contrat HTTP** : 2xx = `ApiResponse<T>` ; erreurs = `ProblemDetail` (RFC7807)

## Admin feature boundaries

Admin controllers follow the same layer ownership rules as the rest of the backend:

- Mono-domain tenant admin CRUD lives in the owning core under `core/<bc>/infra/web/admin`.
- Mono-catalog platform admin CRUD lives in the owning catalog under `catalog/<bc>/internal/web`.
- `features/tenantadmin` and `features/platformadmin` are reserved for composite BFF endpoints that aggregate at least two cores/catalogs.

Current notable placements:

| Surface                        | Active placement                                                  |
| ------------------------------ | ----------------------------------------------------------------- |
| Tenant admin users             | `core.tenantuser.infra.web.admin.TenantUserAdminController`       |
| Tenant admin outlets           | `core.outlet.infra.web.admin.OutletAdminController`               |
| Tenant admin terminals         | `core.terminal.infra.web.admin.TerminalAdminController`           |
| Tenant admin limits            | `core.limitpolicy.infra.web.admin.LimitPolicyAdminController`     |
| Tenant admin autonomy          | `core.autonomy.infra.web.admin.AutonomyAdminController`           |
| Tenant admin policies overview | `features.tenantadmin.policies.web.TenantAdminPoliciesController` |
| Platform admin i18n            | `catalog.i18n.internal.web.PlatformI18nOverridesController`       |
| Platform admin settings        | `catalog.settings.internal.web.PlatformSettingsController`        |
| Platform admin theme presets   | `catalog.theme.internal.web.ThemeAdminController`                 |
| Platform admin overview        | `features.platformadmin.overview.PlatformAdminOverviewController` |

## Hiérarchie de documentation (source de vérité)

En cas de conflit :

1. `docs/ARCHITECTURE.md` (structure & frontières)
2. `docs/PLAYBOOK.md` (workflow & DoD)
3. `docs/conventions/*` (normes techniques détaillées)
4. OpenSpec (dérivé, jamais source de vérité)

---

## 1) 4-Layer Architecture

```
common/       ← Technical/transversal (Bus, EventPublisher, TypedIDs, Cache, TchContext)
catalog/      ← Reference data / lookup (read-mostly, no domain events)
core/         ← Business domains (sales, draw, drawresult, uslottery, haiti)
features/     ← Vertical slices / BFF (ops, tenantadmin, publicdraw, reporting)
```

### Layer Rules

- **common**: Technical only. No domain, no events. Shared by all layers.
- **catalog**: Read-mostly reference data. Admin CRUD via `/platform/**` (SUPER_ADMIN). No business invariants. No events.
- **core**: Domain logic, aggregates, value objects. Hexagonal architecture (domain, application, infra). Events published.
- **features**: Orchestration, BFF, vertical slices. Multi-domain composition. Thin application layer.

---

## 2) Hexagonal Pattern (per core domain)

Each core domain follows a three-layer hexagonal structure:

```
<domain>/domain/           ← Pure domain model (no Spring, no JPA)
<domain>/application/      ← Use cases, command/query handlers, ports
<domain>/infra/            ← Spring beans, JPA entities, controllers, adapters
```

### domain/

- Aggregates, value objects, domain services
- Pure Java (no Spring, no persistence framework)
- Immutable where possible
- Business rules and invariants

### application/

- Command/Query models (records)
- CommandHandler / QueryHandler implementations
- Output ports (interfaces)
- Business logic orchestration (optional domain services)

### infra/

```
infra/
  ├── web/                 ← REST controllers (HttpRequest → Command/Query → Response)
  ├── persistence/         ← JPA entities, repositories, adapters
  ├── adapter/             ← Outbound adapters (HTTP clients, cache, queues)
  ├── event/               ← Event publishers, listeners
  ├── batch/               ← Scheduled jobs, bulk operations
  └── config/              ← Spring @Configuration, beans
```

---

## 3) API Routing Convention

All HTTP endpoints are prefixed with:

```
${app.base-path}/${app.api-version}  (default: /api/v1)
```

### Scope-based path prefixes

- **PUBLIC**: `/api/v1/public/**` — no auth required
- **TENANT**: `/api/v1/tenant/**` — tenant-scoped context (from JWT)
- **ADMIN**: `/api/v1/admin/**` — tenant admin scope
- **PLATFORM**: `/api/v1/platform/**` — platform-level admin (SUPER_ADMIN role)
- **SDR**: `/api/v1/_sdr/**` — Spring Data REST (platform reference data CRUD)

No "naked" routes (`/tenant`, `/draw`, etc.) by convention.

---

## 4) Controller Ownership

- **Domain-scoped controllers** live in `<domain>/infra/web/`
- **Composite BFF endpoints** live in `features/<slice>/infra/web/`
- Controllers are thin: validation + context injection + dispatch → Command/Query

Controllers may not contain business logic.

---

## 5) Typed IDs (Wrappers)

**Golden rule**: Outside persistence, never use raw `UUID`.

- **Domain/Application/DTOs**: Typed ID wrappers (`TenantId`, `TicketId`, `DrawId`, etc.)
- **UUID raw**: Only in JPA entities, repositories, SQL, Flyway

Example:

```java
// ✅ Domain
public record SellTicketCommand(TenantId tenantId, TerminalId terminalId, ...) {}

// ✅ JPA entity
@Entity
public class TicketJpaEntity {
  private UUID id;  // raw UUID here is OK
}

// ✅ Repository interface (output port)
public interface TicketWriterPort {
  Ticket save(Ticket t);  // returns Ticket with typed IDs
}
```

See: `docs/conventions/typed_ids.md`

---

## 6) CQRS — Command/Query Separation

### Commands

- Record class (`public record XxxCommand(...)`)
- Handler: `@UseCase` class implementing `CommandHandler<XxxCommand, XxxResult>`
- Marked with `@TchTx` for write operations
- Return a result object or DTO
- Publish events via `AfterCommit.run()`

### Queries

- Record class (`public record XxxQuery(...)`)
- Handler: `@UseCase` class implementing `QueryHandler<XxxQuery, XxxResult>`
- No `@TchTx` needed (read-only)
- Return a view/DTO (never a domain entity)
- No side-effects

---

## 7) Request Context & Security

- `TchRequestContext` holds tenant, user, roles
- Injected via `@CurrentContext` annotation
- RLS (Row-Level Security) filters data at DB level
- Never pass `tenantId` from client to DB

See: `docs/conventions/context.md`, `docs/conventions/rls.md`

---

## 8) Error Handling

- **2xx responses**: `ApiResponse<T>` or legacy DTO (auto-wrapped by `ApiResponseBodyAdvice`)
- **4xx/5xx responses**: `ProblemDetail` (RFC7807) in `application/problem+json`
- Throw `ProblemRest.*()` exceptions from handlers
- Do not wrap errors in `ApiResponse`

See: `docs/conventions/web_api.md`, `docs/conventions/api_response.md`

---

## 9) Pagination

All list endpoints use standard pagination:

- Request: `TchPageRequest` (via `@TchPaging` annotation)
- Response: `TchPage<T>` (wrapper with totalElements, pages, etc.)

See: `docs/conventions/pagination.md`

---

## 10) Event Publishing & After-Commit

**Principle**: Domain publishes events, infrastructure reacts, **publication happens after commit**.

```java
AfterCommit.run(() -> publisher.publish(event));
```

**Typical use cases**:

- Sale → ledger + stats
- DrawResultApplied → ticket settlement
- DrawSettled → payout finalization

See: `docs/conventions/handler_command.md`, `docs/conventions/idempotency.md`

---

## 11) Persistence & Migrations

- **ddl-auto**: `validate` (never auto-create)
- **Migrations**: Flyway (classpath:db/migration)
- **Entities**: JPA with `@Entity`, soft-delete support
- **Tenant-scoped tables**: `tenant_id` column + RLS policies
- **Audit**: Envers for critical tables (configurable)

See: `docs/conventions/persistence.md`, `docs/conventions/jpa_entities.md`, `docs/conventions/rls.md`

---

## 12) Batch & Scheduled Jobs

- Jobs live in `<domain>/infra/batch/`
- Auto-run disabled (`spring.batch.job.enabled: false`)
- Orchestration via scheduler or ops endpoints
- Context set manually for batch: `TchContext.set(ctx)`
- Cross-domain side-effects via after-commit events

See: `docs/conventions/batch.md`

---

## 13) Cache (Caffeine + Redis)

Two-level caching:

- **L1**: Caffeine (local JVM)
- **L2**: Redis (optional, feature-flagged)

- TTL and specs via `CacheSpecProvider` (no hardcoding)
- Feature flag: `tch.cache.redis.enabled`
- No cache on critical money operations

See: `docs/conventions/cache.md`

---

## 14) Spring Data REST (SDR)

**Acceptable use**:

- Simple admin CRUD on `catalog` reference data
- Endpoints under `/api/v1/_sdr/**`
- Security: SUPER_ADMIN role

**Forbidden**:

- Exposing core aggregates via SDR
- Mixing domain endpoints with SDR

See: `docs/conventions/routing_and_path.md`

---

## 15) Convention over Configuration

- Use naming conventions (see `docs/conventions/naming.md`)
- Follow `Rule of 3` for sub-package creation (don't over-modularize)
- If a local pattern exists, use it (don't invent new architecture)
- Document deviations in CLAUDE.md or issue comments

---

## Additional Resources

- `docs/PLAYBOOK.md` — How to add features, DoD, templates
- `docs/AGENTS.md` — Multi-agent orchestration rules
- `docs/conventions/*` — Technical details per concern
- `VERSIONS.md` — Runtime, framework, library versions
- Each core domain has its own `CLAUDE.md` for scope & rules

---

**Last reviewed**: 2026-01-20  
**Status**: NORMATIVE (non-negotiable rules for all contributors)
