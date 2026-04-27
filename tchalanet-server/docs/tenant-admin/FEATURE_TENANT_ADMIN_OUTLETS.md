# FEATURE_TENANT_ADMIN_OUTLETS

> **Scope**: Backend (`tchalanet-server`)
> **Feature**: Tenant Admin
> **Slice**: `outlets` > **Module**: `com.tchalanet.server.features.tenantadmin.outlets` > **Status**: **NORMATIVE** > **Style**: Feature / Vertical Slice
> **Owns**: UI orchestration only
> **Owns NO business truth**

---

## 1. Purpose

This slice represents the **Outlets (Points of Sale / PDV) management area** of the Tenant Admin portal. It allows a tenant administrator to:

- create and manage outlets
- configure outlet-level parameters
- view outlet status and summaries

This slice exists **only for UI orchestration**.

---

## 2. Source of truth (OWNING CORE)

### `core.outlet` (or equivalent domain)

Owns:

- outlet lifecycle
- outlet tenant-scoped persistence
- outlet invariants and validation rules
- outlet state transitions

---

## 3. Non-duplication rule (CRITICAL)

The `tenantadmin/outlets` slice MUST NOT:

- store outlet data
- validate outlet rules
- manage outlet lifecycle directly
- access repositories or entities

If an outlet-related use-case is missing: ➡️ it MUST be added to `core.outlet`.

---

## 4. Responsibilities of the feature slice

The slice MAY:

- orchestrate outlet creation/update via core commands
- list outlets via core queries
- compose outlet dashboards and summaries
- expose BFF endpoints aligned with admin screens

The slice MUST NOT:

- implement business rules
- bypass core transaction boundaries

---

## 5. Canonical UI flows

### 5.1 Create or update outlet

1. Call `core.outlet.CreateOutletCommand`
2. Call `core.outlet.UpdateOutletCommand`
3. Return `OutletView`

---

### 5.2 List outlets of a tenant

1. Call `core.outlet.ListOutletsQuery`
2. Assemble `OutletListView`

---

## 6. API surface (BFF-style)

> Logical intent only.

- `GET /tenant/admin/outlets`
- `POST /tenant/admin/outlets`
- `PUT /tenant/admin/outlets/{outletId}`
- `GET /tenant/admin/outlets/{outletId}`

---

## 7. Internal structure

The slice follows **Feature Rules (81)**:

- `web` — controllers
- `app` — orchestration services
- `model` — UI models
- `mapper` — assembly

**Rule of 3 applies**.

---

## 8. Typed IDs & RLS

- Typed IDs are mandatory
- No UUID leakage
- Tenant isolation via RLS and request context

---

## 9. TL;DR

- Outlet truth lives in `core.outlet`
- Feature composes and exposes UI flows
- No persistence, no rules in the feature
