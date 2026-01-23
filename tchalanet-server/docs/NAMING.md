# Naming Conventions (Server)

> **Status**: NORMATIVE  
> **Applies to**: tchalanet-server (common/core/catalog/features)  
> **Goal**: consistent naming to reduce cognitive load, improve grep-ability, and prevent architecture drift.

---

## 1) Principles (MUST)

- Names MUST be **searchable** (grep-friendly) and **predictable**.
- Names MUST encode **intent**, not implementation details.
- Prefer **domain language** (Ubiquitous Language) over technical slang.
- Avoid abbreviations unless they are project-standard (ex: RLS, SDR, API).
- One concept = one term (no synonyms competing in code).

---

## 2) Packages & modules

### 2.1 Top-level modules (fixed)

- `common/` = transversal technical only
- `catalog/` = read-mostly reference data shared across domains
- `core/` = critical domains (money/tickets/draw/audit/security/limits)
- `features/` = orchestration / BFF / multi-domain composition

### 2.2 Bounded context package naming

Use lowercase, single segment:

- `core.sales`
- `core.draw`
- `core.audit`
- `core.accesscontrol`

Avoid:

- plural packages (`saleses`), mixed separators, or “impl” folders.

---

## 3) Class naming patterns (by responsibility)

### 3.1 Commands / Queries

- Commands (write): `XxxCommand`
- Queries (read): `XxxQuery`

Examples:

- `SellTicketCommand`
- `CancelTicketCommand`
- `GetTicketStatusQuery`
- `ListDrawsQuery`

Rules:

- Commands/Queries MUST be `record`.
- MUST use typed IDs (no UUID).

### 3.2 Handlers (use cases)

- Command handler: `XxxCommandHandler`
- Query handler: `XxxQueryHandler`

Examples:

- `SellTicketCommandHandler`
- `ListDrawsQueryHandler`

If your project uses the shorter `XxxHandler` today, keep it **only if consistent within the bounded-context**.
Rule: do not mix both styles in the same BC.

### 3.3 Ports

- Read port: `XxxReaderPort`
- Write port: `XxxWriterPort`
- Repository-like port: `XxxRepositoryPort` (only if it truly represents repository semantics)
- External systems: `XxxClientPort` / `XxxGatewayPort` (choose one per BC and stick to it)

Examples:

- `TicketReaderPort`, `TicketWriterPort`
- `AuditEventWriterPort`
- `ExternalResultsFetchPort`

### 3.4 Adapters (infra)

Prefer explicit suffix:

- JPA adapter: `XxxJpaAdapter`
- JDBC adapter: `XxxJdbcAdapter`
- External HTTP adapter: `XxxHttpClient`
- Cache adapter: `XxxCacheAdapter`

Spring Data repository interface:

- `XxxJpaRepository` (Spring Data interface)
  JPA entity:
- `XxxJpaEntity`

Mapper:

- `XxxMapper` (MapStruct mapper)
- Use `CommonIdMapper` in `uses = ...` (already standardized)

### 3.5 Web controllers

- `XxxController` (default)
- If scope matters: prefix the class name by scope only when needed:
  - `PublicXxxController`
  - `AdminXxxController`
  - `TenantXxxController`
  - `PlatformXxxController`

Rule:

- Controller names should remain stable even if the route changes.

### 3.6 DTOs (web layer)

Requests:

- `XxxRequest`
  Responses:
- `XxxResponse`
  List items:
- `XxxItemResponse` (for list rows)

Avoid leaking internal “View” naming into web DTOs.
If you use projections in application/query layer:

- `XxxView` (application-side)
- `XxxRow` for list rows (optional)

---

## 3.7 Scheduling / batch / listeners / external clients naming

### Schedulers

- Name: `XxxScheduler` (or `XxxJobScheduler` if multiple jobs inside)
- Job trigger methods: `tick()`, `runOnce()`, `schedule...()`
- Keys: use `jobKey` naming consistently (see BATCH.md)

Examples:

- `ExternalResultsScheduler`
- `DrawSettlementScheduler`

### Config classes

- Spring config: `XxxConfig`
- Properties: `XxxProperties`
- YAML grouping keys MUST match the properties prefix.

Examples:

- `DrawResultsConfig`, `UsLotteryProvidersConfig`
- `DrawResultsProperties`

### Event listeners

- Listener class: `XxxEventListener`
- Method names: `onXxx(...)` (past tense event)

Examples:

- `DrawResultAppliedEventListener` with `onDrawResultApplied(...)`

### External clients

- Low-level HTTP client: `XxxProviderClient` or `XxxHttpClient` (pick one and keep it)
- If multiple providers: `NyUsLotteryClient`, `FlUsLotteryClient` etc.
- Mapping DTOs:
  - request: `XxxRequestDto`
  - response: `XxxResponseDto`
  - raw provider payload: `XxxProviderDto`

### Domain models

- Aggregates/entities: domain language nouns (`Ticket`, `Draw`, `DrawResult`, `ResultSlot`)
- Value objects: `Xxx` (no suffix) unless needed (`Money`, `Stake`, `Outcome`)
- Domain services: `XxxService` only if truly domain-level and pure.

## 4) Routes & scopes naming

### 4.1 Controller `@RequestMapping` prefixes

Because `spring.mvc.servlet.path = /api/v1`, controllers MUST NOT include `/api/v1` in mappings.

Use ONLY:

- `/public/...`
- `/platform/...`
- `/admin/...`
- `/tenant/...`
- `/_sdr/...` (Spring Data REST only)

Example:

- ✅ `@RequestMapping("/tenant/tickets")`
- ❌ `@RequestMapping("/api/v1/tenant/tickets")`

### 4.2 Endpoint naming

- Use **nouns** for resources (`/tickets`, `/draws`, `/outlets`)
- Use sub-resources for relationships (`/tickets/{id}/cancel`)
- Prefer explicit action endpoints only when the operation is not CRUD (`/approve`, `/settle`, `/void`)

---

## 5) Typed IDs naming (wrappers)

### 5.1 Wrapper names

- Suffix MUST be `Id`: `TenantId`, `TicketId`, `PayoutId`, ...
- File name matches record name.

### 5.2 Wrapper methods (standard)

- `of(UUID)`
- `nullableOf(UUID)`
- `parse(String)`

Converters:

- `StringToXxxIdConverter`

MapStruct helper:

- `CommonIdMapper` uses `mapToXxxId` / `mapFromXxxId`

---

## 6) Events naming

Domain events:

- Past tense: `XxxCreatedEvent`, `XxxCancelledEvent`, `DrawResultAppliedEvent`
- Use “Applied/Requested/Approved/Rejected” consistently.

Publishing rule remains:

- publish `AfterCommit`

---

## 7) Persistence naming

### 7.1 Tables & columns (SQL)

- Tables: `snake_case`
- Columns: `snake_case`
- Foreign keys: `<ref>_id`
- Tenant column: `tenant_id`
- Soft delete: `deleted_at`
- Audit timestamps: `created_at`, `updated_at`
- Optimistic lock: `version`

### 7.2 Flyway scripts

- `V###__short_snake_case_name.sql`

Examples:

- `V040__rls_policies.sql`
- `V052__create_page_model.sql`

---

## 8) Tests naming

- Test class: `XxxTest` (unit), `XxxIT` (integration)
- Test method: camelCase (Java)
- `@DisplayName("should <expected> when <condition>")` is the canonical description.

---

## 9) Anti-patterns (forbidden)

- “Impl” suffix everywhere (`TicketServiceImpl`) unless you have an interface that truly needs it.
- Abbreviations invented locally (`TkCtl`, `TixSvc`).
- Two different names for the same concept (`Shop` vs `Outlet`).
- Routes containing `/api/v1` inside controllers.

---

## 10) Touchpoints

- Routing scopes: `ROUTING_AND_API_PATHS_V1.md`
- Web API conventions: `WEB_API.md`
- Typed IDs policy: `TYPED_IDS.md`
- RLS architecture: `RLS.md`
- Testing rules: `TESTING.md`
