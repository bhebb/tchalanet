# OpenSpec — Feature Rules (81)

> **Scope**: Backend (`tchalanet-server`)
> **Applies to**: all modules under `com.tchalanet.server.features.*` > **Status**: **NORMATIVE** > **Purpose**: structural + architectural rules (**NOT functional**)
> **Style**: Vertical Slice / BFF (non-hexagonal)

---

## 1. What a Feature module IS (definition)

A **Feature module** represents a **UI-oriented use-case boundary**.

A feature exists because:

- there is a UI screen, flow, or navigation entry
- the workflow crosses one or more core domains
- the UI requires orchestration and aggregation

A feature module owns:

- orchestration logic (flow coordination)
- cross-domain composition
- UI-friendly read-models
- BFF endpoints (public / tenant / admin / platform)

A feature is **NOT** a business domain.

---

## 2. What a Feature module is NOT

A feature module MUST NOT:

- define business invariants
- own lifecycle rules
- manage transactional consistency
- implement aggregate state machines
- write directly to the database
- access repositories or JPA entities
- act as a source of truth

If logic must remain valid **without UI**, it does **NOT** belong in a feature.

---

## 3. Architectural style (KEY DECISION)

Feature modules follow **Vertical Slice Architecture**.

They DO NOT follow:

- Hexagonal architecture
- Layered domain architecture
- Port/Adapter formalism

Rationale:

- Features are **product-oriented**, not domain-oriented
- Enforcing hexagonal structure would introduce fake abstractions
- Simplicity and locality are prioritized over theoretical purity

---

## 4. Dependency rules (STRICT)

Allowed dependencies (visual):

common ↑ catalog
core ↑ ↑ └──────── features

Rules:

- features MAY depend on `core` (commands/queries)
- features MAY depend on `catalog` (read-only)
- features MAY depend on `common` (technical utilities)

Forbidden:

- core → features
- catalog → features
- features → repositories (core or catalog)
- features → JPA entities

---

## 5. Feature responsibilities (ALLOWED)

A feature module MAY:

- orchestrate multiple core commands
- orchestrate multiple core/catalog queries
- sequence calls across domains
- aggregate results into UI models
- expose endpoints oriented around screens or flows
- build page-level payloads (dashboards, wizards, summaries)

A feature module orchestrates — **it never decides**.

---

## 6. Feature responsibilities (FORBIDDEN)

A feature module MUST NOT:

- validate business invariants
- bypass core transaction boundaries
- mutate domain state manually
- compute money, limits, or payouts
- enforce security or permission rules internally
- reimplement domain logic already present in core

---

## 7. Package organization (CANONICAL)

### 7.1 Feature root

`com.tchalanet.server.features.<feature_key>`

Rules for `<feature_key>`:

- lowercase
- no hyphens
- semantic UI meaning

Examples:

- `publichome`
- `tenantadmin`
- `ticketverify`

---

### 7.2 Umbrella features (MANDATORY RULE)

If a feature exposes **multiple UI areas or menu entries**, it MUST be split into **sub-slices**:

`features/<feature_key>/<slice_key>`

Rules for `<slice_key>`:

- represents a coherent UI area
- maps to menu / navigation
- lowercase, no hyphens

Example (`tenantadmin`):

- `users`
- `tenantconfig`
- `outlets`
- `terminals`
- `draws`

🚫 A single “mega-service” per feature is forbidden.

---

## 8. Internal roles inside a slice

Slices organize code by **role**, not by layer. Canonical roles:

- `web` — HTTP controllers (BFF boundary)
- `app` — orchestration services
- `model` — UI contracts (request / response / view)
- `mapper` — mapping / assembly logic
- `dynamic` — providers / plug-ins (OPTIONAL)
- `shared` — internal helpers (OPTIONAL, non-domain)

---

## 9. Rule of 3 (PACKAGE CREATION RULE)

A role-specific package (`web/`, `app/`, `model/`, etc.) is created **ONLY IF** it contains **at least 3 elements**.

Rules:

- `< 3 classes` → keep flat at slice level
- `≥ 3 classes` → create the dedicated package

Rationale:

- avoid artificial micro-architecture
- keep small slices readable
- allow natural growth without refactor pressure

This rule applies independently to each role.

---

## 10. Naming conventions (MANDATORY)

### Controllers

- suffix: `Controller`
- example: `TenantUsersController`

### Orchestration services

- suffix: `Service` or `Orchestrator`
- example: `TenantUsersService`

### Models (UI contracts)

- input: `XxxRequest`
- output: `XxxResponse`
- read-models: `XxxView`, `XxxItem`, `XxxSummary`

🚫 The term **DTO** is forbidden in features.

### Mappers

- suffix: `Mapper` or `WebMapper`

### Providers

- interface: `XxxProvider`
- implementation: `XxxProviderImpl`

---

## 11. Controllers (BFF boundary rules)

Controllers MUST:

- be thin
- perform validation and mapping only
- delegate orchestration to services or Command/QueryBus
- return standardized API responses

Controllers MUST NOT:

- contain business logic
- access repositories
- manage transactions
- bypass core handlers

---

## 12. Orchestration rules

Feature orchestration services:

- MAY be sequential
- MAY call multiple domains
- MUST remain explicit and readable
- MUST delegate all mutations to core commands

Feature orchestration MUST NOT:

- introduce implicit transactions
- rely on partial domain state
- cache business decisions

---

## 13. Tenant & RLS awareness

- Feature modules rely on request context for tenant resolution
- Tenant isolation is enforced by RLS at database level
- Features MUST NOT compute tenant filters manually
- SUPER_ADMIN overrides are handled by global context rules

---

## 14. Typed IDs (MANDATORY)

- Feature modules MUST use typed ID wrappers
- UUID usage is forbidden outside persistence
- No String/UUID leakage in feature APIs

---

## 15. Documentation (REQUIRED)

Each feature MUST include: `FEATURE_<FEATURE_NAME>.md`

This document describes:

- UI purpose
- sub-slices (if umbrella)
- core domains touched
- orchestration flows

🚫 Feature documentation MUST NOT describe business invariants.

---

## 16. Enforcement

Architecture enforcement MUST ensure:

- no repository access from features
- no JPA entities in features
- no business logic in controllers
- correct dependency direction
- adherence to Rule of 3

Violations require:

- refactor, OR
- explicit ADR with justification

---

## 17. Mental model (TL;DR)

- **Feature** = UI flow orchestration
- **Core** = business truth
- **Catalog** = reference data
- **Common** = technical glue

If removing the UI removes the value of the code → it belongs in a feature.
