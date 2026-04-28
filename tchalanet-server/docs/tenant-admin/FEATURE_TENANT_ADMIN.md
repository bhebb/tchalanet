# FEATURE_TENANT_ADMIN

> **Scope**: Backend (`tchalanet-server`)
> **Module**: `com.tchalanet.server.features.tenantadmin` > **Status**: **NORMATIVE** > **Architecture style**: Feature / Vertical Slice (UI Portal)
> **Depends on**: `core/*`, `catalog/*`, `common/*` > **Owns**: orchestration + UI composition only
> **Owns NO business truth**

---

## 1. Purpose

`tenantadmin` is an **umbrella feature** that represents the **Tenant Administration Portal**. It exists **only** because there is an admin UI where a tenant operator can:

- configure the tenant
- access user, outlet, and terminal administration surfaces owned by their cores
- configure draws and operational settings
- access tenant-level dashboards and bootstrap data

`tenantadmin` is **NOT a domain**. It is a **composition layer** over multiple core domains.

---

## 2. Non-negotiable principle (NO DUPLICATION)

> **Every tenant configuration rule belongs to exactly ONE core domain.**

- `tenantadmin` MUST NOT re-implement logic
- `tenantadmin` MUST NOT store state
- `tenantadmin` MUST NOT invent invariants
- If a rule/use-case does not exist in a core → **it must be added to the owning core**, not to the feature

The feature only:

- orchestrates existing core handlers
- exposes screen-oriented BFF endpoints
- assembles UI-friendly models

---

## 3. Architectural role

`tenantadmin` is a **UI Portal Feature**:

- single entry point for tenant admin UI
- decomposed internally into **sub-slices**, each aligned with a menu area
- each sub-slice orchestrates one or more core domains

There is **no “TenantAdminService” mega-orchestrator**.

---

## 4. Active decomposition (sub-slices)

The feature contains only UI-composition slices. Mono-domain CRUD controllers live in the owning core under `core/<bc>/infra/web/admin`.

Current filesystem layout:

```
features/tenantadmin/
  ├─ config/
  │  ├─ identity/
  │  ├─ i18n/
  │  └─ settings/
  ├─ policies/
  │  ├─ model/
  │  └─ web/
  └─ FEATURE_TENANT_ADMIN.md
```

Each sub-slice:

- represents a **coherent UI area**
- owns its own controllers, orchestration, and models
- follows **Feature Rules (81)** including the **Rule of 3**

Migrated mono-domain slices:

| Former feature slice                 | Current owner                                                 |
| ------------------------------------ | ------------------------------------------------------------- |
| `tenantadmin/outlets`                | `core.outlet.infra.web.admin.OutletAdminController`           |
| `tenantadmin/terminals`              | `core.terminal.infra.web.admin.TerminalAdminController`       |
| `tenantadmin/users`                  | `core.tenantuser.infra.web.admin.TenantUserAdminController`   |
| `tenantadmin/policies` limits CRUD   | `core.limitpolicy.infra.web.admin.LimitPolicyAdminController` |
| `tenantadmin/policies` autonomy CRUD | `core.autonomy.infra.web.admin.AutonomyAdminController`       |

The remaining `tenantadmin/policies` feature exposes only the composite policies overview because it aggregates limit policy and autonomy data.

---

## 5. Ownership matrix (SOURCE OF TRUTH)

### 5.1 Users & Identity (`core.tenantuser.infra.web.admin`)

**Owning cores**:

- `core.user`
  - CRUD user
  - user preferences (UI preferences)
- `core.tenantuser`
  - assign / unassign user ↔ tenant
  - list users of a tenant
  - `/me` (current user)
  - `/bootstrap` (startup payload)

**Feature role**:

- none for mono-domain CRUD; the tenant-scoped admin API is owned by `core.tenantuser`
- any future feature layer may only compose data from multiple cores

🚫 The feature MUST NOT:

- manage user persistence
- manage membership tables
- store preferences

---

### 5.2 Tenant configuration (`tenantadmin/config`)

**Owning cores / catalogs** (examples):

- `core.tenant` (tenant lifecycle / policies)
- `catalog.tenant` (defaults, presets, reference config)
- other dedicated cores for policies (limits, workflows, etc.)

Examples:

- branding / theme
- language & i18n surface config
- feature flags surface
- high-level tenant settings

**Feature role**:

- expose configuration screens
- orchestrate read/write commands
- assemble configuration views

🚫 The feature MUST NOT:

- become the source of truth for tenant config
- define validation rules

---

### 5.3 Outlets (`core.outlet.infra.web.admin`)

**Owning core**: `core.outlet` (or equivalent)

**Owns**:

- outlet lifecycle
- tenant-scoped outlet configuration

**Admin API role**:

- list outlets
- create/update via core commands
- expose tenant-scoped outlet administration endpoints

---

### 5.4 Terminals (`core.terminal.infra.web.admin`)

**Owning core**: `core.terminal` (or equivalent)

**Owns**:

- terminal lifecycle
- pairing / activation
- security keys

**Admin API role**:

- expose pairing screens
- show terminal state
- orchestrate setup flows

---

### 5.5 Policies overview (`tenantadmin/policies`)

**Owning cores**:

- `core.limitpolicy`
- `core.autonomy`

**Owns**:

- limit definitions and assignments in `core.limitpolicy`
- autonomy rules in `core.autonomy`

**Feature role**:

- expose the composite `GET /admin/policies/overview` payload
- aggregate limit policy and autonomy read models for the Tenant Admin portal

CRUD endpoints for limit policies and autonomy rules MUST remain in their owning cores.

---

### 5.6 Draw configuration

> Draw tenant-admin implementation is not currently present under `features/tenantadmin`. Add it only through an approved OpenSpec change when the UI composition requires it.

**Owning core**: `core.draw`

**Owns**:

- tenant draw channels
- draw windows
- activation/deactivation rules

**Feature role**:

- configure draws for a tenant
- list current / upcoming draws
- orchestrate multi-step configuration flows

---

## 6. `/me` and `/bootstrap` (IMPORTANT)

### Source of truth

- `/me` → **`core.tenantuser`**
- `/bootstrap` → **`core.tenantuser`**

These endpoints:

- describe the **current authenticated user**
- include tenant context
- are used by applications at startup

### Feature usage

- `tenantadmin` MAY **relay or extend** these payloads
- `tenantadmin` MUST NOT redefine their meaning
- any additional data MUST be clearly composed from other cores

---

## 7. Internal structure per sub-slice

Each sub-slice follows **Feature Rules (81)**:
Roles:

- `web` — controllers
- `app` — orchestration services
- `model` — UI contracts
- `mapper` — assembly
- optional `shared` / `dynamic`

**Rule of 3 applies**: packages are created only if ≥ 3 elements.

---

## 8. Orchestration rules

Within `tenantadmin`:

- orchestration MAY call multiple core handlers
- orchestration MAY be sequential
- each mutation MUST go through a core command
- failures MUST respect core transaction boundaries

🚫 No cross-core transaction coupling is allowed.

---

## 9. Dependency & access rules

Allowed:

- `tenantadmin → core`
- `tenantadmin → catalog`
- `tenantadmin → common`

Forbidden:

- `tenantadmin → repositories`
- `tenantadmin → JPA entities`
- `tenantadmin → direct DB access`
- `core → tenantadmin`

---

## 10. Typed IDs & RLS

- All APIs use typed ID wrappers
- No UUID leakage
- Tenant isolation relies on RLS and request context
- Feature MUST NOT compute tenant filters manually

---

## 11. Documentation requirements

This document defines:

- structural rules
- ownership boundaries
- orchestration responsibility

Business rules, invariants, and lifecycle remain documented in:

- `DOMAIN_<X>.md` inside each core

---

## 12. Mental model (TL;DR)

- **Tenant Admin = portal**
- **Cores = truth**
- **Features = composition**
- **No duplication, even if it means more handlers in core**

If a rule exists in `tenantadmin`, it is a bug.

---

_End of document._
