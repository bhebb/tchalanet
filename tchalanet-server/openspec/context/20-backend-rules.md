# Backend Rules & Map

This file defines the backend architecture rules and navigation map.
It is intentionally concise.

Detailed implementation lives in `tchalanet-server/docs/`.

---

## Architecture

- Spring Boot
- Hexagonal architecture (Ports & Adapters)
- CQRS enforced via `CommandBus` / `QueryBus`

Rules:

- Business logic lives in `core/`
- Orchestration lives in `features/`
- Technical glue lives in `common/`
- Reference data lives in `catalog/`

---

## Multi-Tenant

- PostgreSQL Row-Level Security (RLS)
- Tenant resolved from request context (`TchRequestContext`)
- Client-provided `tenant_id` is NEVER trusted
- RLS is the last line of defense (not routing logic)

---

## API Conventions

- All JSON endpoints return `ApiResponse<T>`
- Collections use `TchPage<T>` (not Spring `Page`)
- Controllers use typed ID wrappers (`TenantId`, `TicketId`, …)
- Controllers remain thin (mapping, validation, delegation only)
- Exception handling:
  - NEVER throw low-level exceptions like `EntityNotFoundException` or `ProblemRest` from domain handlers/use cases.
  - ALWAYS use domain-specific exceptions (e.g., `DrawNotFoundException`).
  - Map domain exceptions to `ProblemDetail` (404, 422, etc.) in the global `ErrorHandler`.

---

## Persistence

- Flyway migrations only (no `ddl-auto=update`)
- UUID allowed ONLY in:
  - JPA entities
  - repositories
  - JDBC adapters
- Typed ID wrappers everywhere else

---

# Norme Repository Read — find vs get

## Règle

## Pattern canonique

```java
@Override
public Optional<Draw> findById(DrawId drawId) {
  Objects.requireNonNull(drawId, "drawId is required");
  return jpa.findById(drawId.value()).map(mapper::toDomain);
}

@Override
public Draw getById(DrawId drawId) {
  return findById(drawId)
      .orElseThrow(() -> new EntityNotFoundException("Draw not found"));
}
```

- findXxx(...) retourne Optional<T>
- getXxx(...) retourne T ou throw
- toujours valider les paramètres avec Objects.requireNonNull
- getXxx(...) doit réutiliser findXxx(...)
- getXxx(..) lève EntityNotFoundException s'il trouve pas l'entité

## Package Structure (STRICT)

Backend code is organized into four layers.

### `common/`

Technical transversal components only:

- command/query bus
- error handling
- context & security filters
- base entities
- auditing
- utilities & infra helpers

Must NOT contain business rules.

---

### `core/`

Critical business domains:

- aggregates & entities
- invariants and state machines
- domain services
- domain events

Domain documentation:

- `tchalanet-server/src/**/DOMAIN_*.md`

---

### `features/`

Application / orchestration layer:

- REST controllers (BFF-style)
- workflows and page models
- cross-domain coordination

Must NOT redefine business invariants.

---

### `catalog/`

Reference & lookup data:

- calendars
- game definitions
- limits & configuration
- external mappings
- static or slowly-changing business data

Rules:

- Side-effect free
- No domain events
- No business invariants

---

## Source of Truth

- Architecture & rules (index): **OpenSpec context packs**
- Technical implementation details: `tchalanet-server/docs/`
- Business rules & invariants: `DOMAIN_*.md` per domain
