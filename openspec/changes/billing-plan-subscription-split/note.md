# billing-plan-subscription-split — Notes & Handoff

> **Change-id**: billing-plan-subscription-split  
> **Status**: APPROVED  
> **Audience**: Backend developers, reviewers, Copilot  
> **Scope**: `catalog/plan` + `core/subscription`

---

## 1. What changed (authoritative)

This change introduces a **strict architectural split** between:

- **Billing Plans** → `catalog/plan`

  - global, stable reference data
  - read-mostly
  - no lifecycle
  - no domain events

- **Tenant Subscriptions** → `core/subscription`
  - tenant-scoped lifecycle
  - transactional
  - versioned / audited
  - idempotent commands
  - emits events after commit

This aligns billing with the same pattern already validated for:

- `catalog/theme` vs `core/tenanttheme`
- `catalog/game` vs `core/tenant-game` (upcoming)

---

## 2. What is now considered correct

### Catalog / Plan

- Plans are **pure reference data**
- Exposed only via `PlanCatalog` (API)
- Reads are cacheable
- Admin writes are internal-only
- No events, no workflows, no tenant logic

### Core / Subscription

- Subscriptions are **tenant lifecycle**
- Commands validate plans via `PlanCatalog` API only
- Persistence is tenant-scoped (RLS)
- Commands are idempotent
- Events are published **after commit**
- Queries are safe for bootstrap usage

### Boundaries (non-negotiable)

- `core/subscription` MUST NOT depend on `catalog/plan/internal/**`
- No JPA entity or repository from catalog used in core
- No cross-table joins between subscription and plan tables
- All inter-domain calls follow `inter_domain_calls.md`

---

## 3. What must be refactored (if present)

If an existing billing module exists, look for:

### Smells indicating refactor is needed

- Plan and subscription stored in the same table
- Plan entity containing tenant state (status, dates)
- Subscription logic inside a catalog module
- Domain events emitted from a catalog
- Core code importing `PlanJpaEntity` or repository
- Controllers mapping entities directly

### Typical moves

- Move plan persistence + read APIs → `catalog/plan`
- Move tenant plan assignment, status, dates → `core/subscription`
- Replace direct DB access with `PlanCatalog.findByCode()`
- Introduce commands/queries for subscription lifecycle

---

## 4. Copilot guidance (do / don’t)

### DO

- Use `PlanCatalog` for plan validation
- Treat plan `code` as the functional identifier
- Implement idempotency on subscription commands
- Publish `TenantSubscriptionUpdatedEvent` after commit
- Enforce RLS on `tenant_subscription`
- Follow typed IDs and command/query conventions

### DO NOT

- Add business logic to `catalog/plan`
- Emit events from catalog
- Join plan tables from subscription persistence
- Reference `catalog/plan/internal/**` from core
- Hard-delete plans

---

## 5. Implementation checklist

### Catalog / Plan

- [ ] Create module structure per `75-catalog-rules`
- [ ] Add `billing_plan` table with `code`, `active`, `deleted_at`
- [ ] Implement `PlanCatalog` (read-only)
- [ ] Implement `PlanAdminService` (internal writes)
- [ ] Add cache names and eviction on writes
- [ ] Mapper unit tests
- [ ] ArchUnit guard: `catalog.plan.api` → no `internal`

### Core / Subscription

- [ ] Create `core/subscription` skeleton
- [ ] Add `tenant_subscription` table (RLS)
- [ ] Implement `ApplyTenantPlanCommandHandler`
- [ ] Implement `ResolveTenantSubscriptionQueryHandler`
- [ ] Enforce idempotency strategy
- [ ] Publish `TenantSubscriptionUpdatedEvent` after commit
- [ ] Integration tests (RLS + idempotency)

### Feature / Bootstrap

- [ ] Include effective subscription in bootstrap response
- [ ] Optionally enrich with plan metadata (via `PlanCatalog`)

---

## 6. Known open decisions (explicit)

- Subscription state machine depth (MVP vs full)
- Policy for inactive plans:
  - reject new assignments?
  - allow for legacy tenants only?
- Whether pricing remains declarative or requires a pricing engine later

These decisions MUST be documented before extending beyond MVP.

---

## 7. Validation

After implementation:

```bash
# Verify catalog structure
ls catalog/plan/api
ls catalog/plan/internal/read

# Run architecture tests
mvn test -Dtest=ArchitectureTest

# Run integration tests
mvn test -Dtest=PlanCatalogIntegrationTest
mvn test -Dtest=SubscriptionCommandHandlerTest
```

8. Outcome

After this change:

Billing plans and subscriptions evolve independently

Catalog/core responsibilities are explicit and enforced

Bootstrap can safely expose subscription state

The system is ready for future billing extensions
(renewals, payments, pricing engine)
