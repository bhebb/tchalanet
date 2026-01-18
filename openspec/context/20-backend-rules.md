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

---

## Persistence

- Flyway migrations only (no `ddl-auto=update`)
- UUID allowed ONLY in:
  - JPA entities
  - repositories
  - JDBC adapters
- Typed ID wrappers everywhere else

---

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
