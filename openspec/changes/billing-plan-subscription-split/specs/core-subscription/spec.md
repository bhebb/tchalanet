# Core Subscription Specification

## Purpose

This specification defines `core/subscription`, responsible for tenant-scoped subscription lifecycle management (plan application, status transitions, events).

It MUST validate plan existence through `catalog/plan` public API only.

---

## ADDED Requirements

### Requirement: S1 — Apply plan to tenant (command)

The system MUST allow applying a billing plan to a tenant via:
`ApplyTenantPlanCommand(tenantId, planCode, effectiveAt?, idempotencyKey?)`.

- Handler MUST validate plan existence via `PlanCatalog.findByCode(planCode)`
  and MUST reject soft-deleted plans.
- Handler MUST persist tenant-scoped subscription state (`tenant_subscription`)
  and increment version.
- After commit, handler MUST publish `TenantSubscriptionUpdatedEvent`.

#### Scenario: apply valid plan

- Given: plan `pro-v1` exists and is active in `catalog/plan`
- When: `ApplyTenantPlanCommand(T, "pro-v1")` is handled
- Then: `tenant_subscription` is updated for tenant T and version increments
- And: `TenantSubscriptionUpdatedEvent` is published after commit

#### Scenario: plan soft-deleted rejected

- Given: plan `old-v0` exists but is soft-deleted (`deleted_at!=NULL`)
- When: `ApplyTenantPlanCommand(T, "old-v0")` is handled
- Then: command is rejected with readable error
- And: no write occurs
- And: no event is published

#### Scenario: plan inactive policy

- Given: plan `legacy-v1` exists with `active=false` and not soft-deleted
- When: `ApplyTenantPlanCommand(T, "legacy-v1")`
- Then: behavior is policy-defined:
  - either reject for new assignments
  - or allow only for existing tenants (migration/legacy)

(Implementation MUST choose and document one policy.)

---

### Requirement: S2 — Resolve tenant subscription (query)

The system MUST expose a query: `ResolveTenantSubscriptionQuery(tenantId)` returning effective subscription view.

- MUST be safe for bootstrap usage (fast, side-effect free).
- MAY enrich with plan metadata by reading `PlanCatalog` (API only).

#### Scenario: resolve existing subscription

- Given: tenant T has an existing subscription row
- When: `ResolveTenantSubscriptionQuery(T)` is executed
- Then: returns subscription view with status, planCode, dates, version

---

### Requirement: S3 — Lifecycle transitions

Subscription status transitions MUST be validated and consistent.

At minimum, module MUST support:

- ACTIVE
- SUSPENDED
- CANCELED/EXPIRED (implementation-defined naming)

Transitions MUST be explicit and validated.

#### Scenario: invalid transition rejected

- Given: subscription is EXPIRED
- When: `SuspendSubscriptionCommand` is executed
- Then: command is rejected as invalid transition

---

### Requirement: S4 — Idempotency

All subscription commands MUST be idempotent. Retries MUST NOT create duplicate final state or duplicate final events.

Idempotency MUST follow the project idempotency convention:

- idempotency key / command id dedup
- or optimistic locking compare-and-set

(Implementation MUST select and apply one strategy consistently.)

#### Scenario: command retried safely

- Given: client retries `ApplyTenantPlanCommand` due to timeout
- When: same command is re-submitted
- Then: resulting subscription state is consistent and event emission is safe

---

### Requirement: S5 — Events (after-commit)

`TenantSubscriptionUpdatedEvent` MUST be published after commit.

Event payload MUST include:

- `tenantId`
- `planCode` (or planId if internally chosen)
- `status`
- `version`
- `timestamp`
- `initiator`

#### Scenario: event published after commit

- Given: apply command succeeds
- When: transaction commits
- Then: event is observable after commit, not before

---

### Requirement: S6 — Inter-domain calls & boundaries

`core/subscription` MUST NOT:

- depend on `catalog/plan/internal/**`
- access catalog tables directly
- use `PlanJpaEntity` or `PlanJpaRepository`

`core/subscription` MUST:

- validate plan existence using `PlanCatalog` public API only.

#### Scenario: boundary enforced

- Given: subscription handler needs plan validation
- When: handler validates plan
- Then: it calls `PlanCatalog.findByCode()` only

---

### Requirement: S7 — Tenant-scoped persistence & RLS

Subscription persistence MUST be tenant-scoped and enforce RLS policies.

Expected table: `tenant_subscription` with `tenant_id` and lifecycle fields, plus `version` and audit columns.

#### Scenario: tenant-scoped visibility

- Given: RLS enabled on `tenant_subscription`
- When: subscription is written for tenant T
- Then: row is visible only to authorized sessions for tenant T

---

### Requirement: S8 — Security & permissions

Writes MUST be authorized:

- Plan CRUD is platform/admin (in catalog)
- Subscription changes are restricted (tenant admin/manager or platform) per policy

Reads needed for bootstrap MUST be permitted for authenticated tenant users.

---

## Non-Functional Requirements

### NF1 — Performance

- Resolve query MUST be suitable for bootstrap.
- Catalog reads SHOULD be cached; query should not create N+1 calls.

### NF2 — Observability

- Commands and events SHOULD be logged with structured metadata (tenantId, planCode, version).

---

## Acceptance Criteria

- [ ] Apply command validates plan via `PlanCatalog` API only
- [ ] Commands are idempotent
- [ ] Event published after commit
- [ ] RLS enforced on `tenant_subscription`
- [ ] No direct dependency on `catalog/plan/internal/**`

---

## Refactoring existing code

**Existing code location**: `catalog/billing`

The following files MUST be refactored according to the new architecture:

**Move to `core/subscription`**:

- Domain model: `Subscription`, `SubscriptionStatus`
- Domain exceptions: `Subscription*Exception`
- Persistence: `SubscriptionJpaEntity` → adapt to `tenant_subscription` table
- Ports: `SubscriptionReaderPort`, `SubscriptionWriterPort`

**Critical changes required**:

1. Replace `PlanId planId` → `String planCode` (soft reference)
2. Validate plan via `PlanCatalog.findByCode()` (public API only)
3. Remove business logic from `Subscription` record → move to command handlers
4. Adapt entity to new table structure (`tenant_subscription` with `plan_code`, `started_at`, `ends_at`, etc.)

**Do NOT move**:

- `BillingProvider` integration → out of scope (separate module)
- Batch jobs → out of scope MVP or move to `core/subscription/infra/batch`

**Detailed guide**: See `REFACTORING_GUIDE.md` in change root.
