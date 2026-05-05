# OpenSpec — Core Rules (80)

> **Scope**: Backend (`tchalanet-server`)  
> **Applies to**: all modules under `com.tchalanet.server.core.*`  
> **Status**: **NORMATIVE**  
> **Purpose**: structural + technical rules (**NOT functional**)

---

## 1. What a Core module IS (definition)

A **Core module** represents a **business domain**.

It owns:

- business invariants
- lifecycle rules
- transactional consistency
- domain decisions
- side-effects and outcomes

A core module is the **source of truth** for a domain.

👉 Functional meaning lives in `DOMAIN_<X>.md`.

---

## 2. What a Core module is NOT

A core module is NOT:

- a reference data catalog
- a CRUD façade
- a pure data registry
- a passive read-only module

If a module:

- only exposes lookup data
- has no lifecycle
- has no invariants
- has no side-effects

➡️ it belongs in **`catalog/`**, not in `core/`.

---

## 3. Mandatory layering (KEY RULE)

Every core module **MUST follow the same internal layering**.

### 3.1 Mandatory package structure

core/<domain>/
├─ domain/
│ ├─ model/
│ ├─ exception/
│ └─ service/ (optional, domain services only)
├─ application/
│ ├─ command/
│ │ ├─ model/
│ │ └─ handler/
│ ├─ query/
│ │ ├─ model/
│ │ └─ handler/
│ └─ event/
├─ port/
│ └─ out/
└─ infra/
├─ persistence/
├─ web/
├─ batch/
├─ event/
└─ cache/

🚫 **Forbidden**:

- skipping layers
- collapsing command/query
- accessing infra from domain

---

## 4. Domain layer (PURE)

The **domain layer** is the heart of the core module.

### 4.1 Domain rules

The domain layer:

- contains **business rules only**
- is framework-free
- is persistence-agnostic
- is deterministic
- uses typed IDs exclusively

🚫 MUST NOT:

- access repositories
- publish events
- access Spring
- depend on `application/` or `infra/`

### 4.2 Domain services

Allowed ONLY when:

- logic does not belong to a single aggregate
- logic is pure and deterministic

🚫 Not allowed:

- orchestration
- IO
- transaction handling

---

## 5. Application layer (CQRS boundary)

The **application layer** orchestrates use-cases.

### 5.1 Commands

Commands:

- represent **intent**
- mutate state
- run in transactions
- validate invariants
- publish domain events (after commit)

**Location**:

- application/command/model
- application/command/handler

Rules:

- command = immutable record
- handler = single responsibility
- one handler per command

---

### 5.2 Queries

Queries:

- represent **questions**
- never mutate state
- never publish events
- are side-effect free

**Location**:

- application/query/model
- application/query/handler

Rules:

- queries MAY use projections
- queries MUST NOT reuse command handlers
- queries MUST respect RLS implicitly

---

### 5.3 Application events

Application events:

- represent **facts that happened**
- are published **after transaction commit**
- trigger side-effects (async or sync)

Rules:

- no business decision in listeners
- idempotent listeners
- failures MUST NOT rollback the command

---

## 6. Ports (Hexagonal)

### 6.1 Output ports ONLY

Core modules define **output ports only**.

**Location**:

- port/out

Ports may represent:

- persistence access
- external providers
- messaging
- clocks
- id generators

🚫 Core MUST NOT define input ports.

---

## 7. Infrastructure layer (Adapters)

Infrastructure implements ports.

### 7.1 Persistence

**Location**:

- infra/persistence

Rules:

- JPA/JDBC entities only
- UUID allowed ONLY here
- mapping isolated
- soft-delete preferred
- RLS enforced at DB level

🚫 Forbidden:

- business logic
- validation rules

---

### 7.2 Web (Controllers)

**Location**:

- infra/web

Rules:

- HTTP boundary only
- delegate to CommandBus / QueryBus
- no logic
- no mapping
- return `ApiResponse<T>`

🚫 Forbidden:

- repository access
- domain access
- transaction handling

---

### 7.3 Batch / Scheduler

**Location**:

- infra/batch

Rules:

- orchestration only
- no business logic
- must use application commands
- must set tenant context explicitly

---

### 7.4 Events (Infra listeners)

**Location**:

- infra/event

Rules:

- subscribe to application events
- idempotent
- no state mutation outside commands

---

## 8. RLS & Tenant scoping (CRITICAL)

### 8.1 Read side

- NO tenant filters in Java code
- tenant scoping is 100% SQL
- repositories rely on `app.current_tenant`

🚫 Forbidden:

- `findByTenantId(...)` in read-side queries

---

### 8.2 Write side

- tenantId MAY be passed explicitly
- write repositories MAY filter by tenant
- admin use-cases MAY override context

---

## 9. Soft delete & lifecycle

Rules:

- `deleted_at` = logical removal
- existence checks MUST use `...AndDeletedAtIsNull`
- commands MUST handle resurrect/recreate logic

Seed/bootstrap MUST reason in terms of **live rows**.

---

## 10. Typed IDs (MANDATORY)

Rules:

- all domain/application layers use typed IDs
- UUID only in infra/persistence
- no String/UUID leakage upward

---

## 11. Dependency rules (STRICT)

Allowed dependencies:
common
↑
catalog core
↑ ↑
└── features

Rules:

- core MAY read from catalog APIs
- core MUST NOT write to catalogs
- core MUST NOT depend on features
- catalog MUST NOT depend on core

---

## 12. Documentation split (VERY IMPORTANT)

This document defines:

- structure
- layering
- technical constraints

`DOMAIN_<X>.md` defines:

- business meaning
- invariants
- lifecycle rules
- examples
- edge cases

🚫 Never mix both.

---

## 13. Enforcement (REQUIRED)

ArchUnit rules MUST enforce:

- no domain → infra access
- no controller → repository access
- no UUID outside infra
- no tenant filters in read-side code
- commands mutate / queries do not

Violations require:

- refactor OR
- explicit ADR

---

## 14. Mental model (TL;DR)

- **Domain** = rules & invariants
- **Command** = intent to change
- **Query** = question
- **Event** = fact
- **Port** = dependency
- **Adapter** = implementation

If a class does not clearly fit one of these roles → it is misplaced.
