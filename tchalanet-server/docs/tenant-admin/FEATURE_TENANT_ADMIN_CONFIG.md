# FEATURE_TENANT_ADMIN_CONFIG

> **Scope**: Backend (`tchalanet-server`)
> **Feature**: Tenant Admin
> **Slice**: `tenantconfig` > **Module**: `com.tchalanet.server.features.tenantadmin.tenantconfig` > **Status**: **NORMATIVE** > **Style**: Feature / Vertical Slice
> **Owns**: UI orchestration only
> **Owns NO configuration truth**

---

## 1. Purpose

This slice represents the **Tenant Configuration area** of the Tenant Admin portal. It exposes UI screens allowing a tenant administrator to:

- configure tenant-level settings
- manage branding and UI preferences
- configure feature exposure and surface configuration

This slice is **not** the source of truth for configuration.

---

## 2. Source of truth (OWNING DOMAINS)

Depending on the configuration type, ownership belongs to:

### 2.1 `core.tenant` (if present)

Owns:

- tenant lifecycle
- tenant-level policies
- mutable tenant state

---

### 2.2 Policy cores (examples)

- `core.limitpolicy`
- `core.salespolicy`
- `core.validationworkflow`
- any other core defining tenant-scoped operational rules

These cores own:

- invariants
- validation rules
- persistence
- side-effects

---

### 2.3 Catalog modules

- `catalog.tenant`
- other catalogs for defaults / presets / registries

Catalogs are:

- read-mostly
- side-effect free
- shared across features

---

## 3. Non-duplication rule (CRITICAL)

The `tenantadmin/tenantconfig` slice MUST NOT:

- store tenant configuration
- define validation rules
- apply defaults on its own
- compute operational decisions

If configuration logic is missing: ➡️ it MUST be implemented in the owning core or catalog.

---

## 4. Responsibilities of the feature slice

The slice MAY:

- orchestrate configuration queries from multiple cores
- expose screen-oriented BFF endpoints
- aggregate config fragments into UI models
- coordinate multi-step configuration flows

The slice MUST NOT:

- validate configuration semantics
- persist configuration state
- enforce policy logic

---

## 5. Canonical configuration categories

Examples of UI configuration categories exposed by this slice:

- branding & theme
- language & localization surface
- feature exposure flags
- operational configuration (delegated to policy cores)

Each category maps to:

- one owning core OR
- one catalog + one core for persistence

---

## 6. API surface (BFF-style)

> Logical endpoints only.

- `GET /tenant/admin/config`
- `PUT /tenant/admin/config/branding`
- `PUT /tenant/admin/config/features`
- `PUT /tenant/admin/config/policies`

Each endpoint:

- delegates mutations to core commands
- returns composed configuration views

---

## 7. Internal structure

The slice follows **Feature Rules (81)**:

- `web` — controllers
- `app` — orchestration services
- `model` — configuration views
- `mapper` — mapping logic

**Rule of 3 applies**.

---

## 8. Typed IDs & RLS

- Typed IDs are mandatory
- No UUID leakage
- Tenant scoping via request context + RLS
- No tenant filtering in Java code

---

## 9. TL;DR

- Config logic lives in cores/catalogs
- This slice is a UI façade
- No defaults, no rules, no persistence
- Tenant Admin remains orchestration-only
